# AWS ECS Deployment Guide for Message Chain API

This guide provides step-by-step instructions to deploy the Message Chain API to AWS ECS (Elastic Container Service) using Fargate.

## Prerequisites

- AWS Account with appropriate permissions
- AWS CLI installed and configured
- Docker installed locally
- Java 21 and Maven installed (for local building)

## Architecture Overview

```
Internet → ALB (Application Load Balancer) → ECS Service (Fargate) → Container
```

## Deployment Steps

### Step 1: Build and Test Locally

First, verify the application works locally:

```bash
# Build the project
mvn clean package

# Run locally
java -jar target/Microservice_demo-0.0.1-SNAPSHOT.jar

# Test the endpoint
curl http://localhost:8080/api/message?user=john
```

Or use Docker:

```bash
# Build Docker image locally
docker build -t message-chain-api:latest .

# Run with Docker
docker run -p 8080:8080 message-chain-api:latest

# Test
curl http://localhost:8080/api/message?user=john
```

### Step 2: Create ECR Repository

Amazon ECR (Elastic Container Registry) will store your Docker images.

```bash
# Set your AWS region
export AWS_REGION=us-east-1

# Create ECR repository
aws ecr create-repository \
    --repository-name message-chain-api \
    --region $AWS_REGION

# Note the repositoryUri from the output, you'll need it
```

Expected output:
```json
{
    "repository": {
        "repositoryUri": "123456789012.dkr.ecr.us-east-1.amazonaws.com/message-chain-api"
    }
}
```

### Step 3: Build and Push Docker Image to ECR

```bash
# Set variables (replace with your values)
export AWS_ACCOUNT_ID=123456789012
export AWS_REGION=us-east-1
export ECR_REPO=$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/message-chain-api

# Authenticate Docker to ECR
aws ecr get-login-password --region $AWS_REGION | \
    docker login --username AWS --password-stdin $ECR_REPO

# Build the Docker image
docker build -t message-chain-api:latest .

# Tag the image for ECR
docker tag message-chain-api:latest $ECR_REPO:latest
docker tag message-chain-api:latest $ECR_REPO:v1.0.0

# Push to ECR
docker push $ECR_REPO:latest
docker push $ECR_REPO:v1.0.0
```

### Step 4: Create ECS Cluster

```bash
# Create ECS cluster
aws ecs create-cluster \
    --cluster-name message-chain-cluster \
    --region $AWS_REGION
```

### Step 5: Create Task Execution Role

ECS tasks need an IAM role to pull images from ECR and write logs to CloudWatch.

```bash
# Create trust policy file
cat > ecs-task-trust-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ecs-tasks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

# Create IAM role
aws iam create-role \
    --role-name ecsTaskExecutionRole \
    --assume-role-policy-document file://ecs-task-trust-policy.json

# Attach AWS managed policy
aws iam attach-role-policy \
    --role-name ecsTaskExecutionRole \
    --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

### Step 6: Create CloudWatch Log Group

```bash
# Create log group for application logs
aws logs create-log-group \
    --log-group-name /ecs/message-chain-api \
    --region $AWS_REGION
```

### Step 7: Create ECS Task Definition

Create a file named `task-definition.json`:

```json
{
  "family": "message-chain-api-task",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::YOUR_ACCOUNT_ID:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "message-chain-api",
      "image": "YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/message-chain-api:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "essential": true,
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "default"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/message-chain-api",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": [
          "CMD-SHELL",
          "wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1"
        ],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

**Replace**:
- `YOUR_ACCOUNT_ID` with your AWS account ID
- Adjust region if needed

Register the task definition:

```bash
aws ecs register-task-definition \
    --cli-input-json file://task-definition.json \
    --region $AWS_REGION
```

### Step 8: Create Application Load Balancer (ALB)

#### 8.1 Create Security Groups

```bash
# Get default VPC ID
export VPC_ID=$(aws ec2 describe-vpcs \
    --filters "Name=isDefault,Values=true" \
    --query "Vpcs[0].VpcId" \
    --output text \
    --region $AWS_REGION)

# Create security group for ALB
export ALB_SG_ID=$(aws ec2 create-security-group \
    --group-name message-chain-alb-sg \
    --description "Security group for Message Chain ALB" \
    --vpc-id $VPC_ID \
    --region $AWS_REGION \
    --query 'GroupId' \
    --output text)

# Allow HTTP traffic to ALB
aws ec2 authorize-security-group-ingress \
    --group-id $ALB_SG_ID \
    --protocol tcp \
    --port 80 \
    --cidr 0.0.0.0/0 \
    --region $AWS_REGION

# Create security group for ECS tasks
export ECS_SG_ID=$(aws ec2 create-security-group \
    --group-name message-chain-ecs-sg \
    --description "Security group for Message Chain ECS tasks" \
    --vpc-id $VPC_ID \
    --region $AWS_REGION \
    --query 'GroupId' \
    --output text)

# Allow traffic from ALB to ECS tasks on port 8080
aws ec2 authorize-security-group-ingress \
    --group-id $ECS_SG_ID \
    --protocol tcp \
    --port 8080 \
    --source-group $ALB_SG_ID \
    --region $AWS_REGION
```

#### 8.2 Create ALB

```bash
# Get subnet IDs (use at least 2 subnets in different AZs)
export SUBNET_IDS=$(aws ec2 describe-subnets \
    --filters "Name=vpc-id,Values=$VPC_ID" \
    --query "Subnets[0:2].SubnetId" \
    --output text \
    --region $AWS_REGION | tr '\t' ',')

# Create Application Load Balancer
export ALB_ARN=$(aws elbv2 create-load-balancer \
    --name message-chain-alb \
    --subnets $(echo $SUBNET_IDS | tr ',' ' ') \
    --security-groups $ALB_SG_ID \
    --region $AWS_REGION \
    --query 'LoadBalancers[0].LoadBalancerArn' \
    --output text)

# Get ALB DNS name
export ALB_DNS=$(aws elbv2 describe-load-balancers \
    --load-balancer-arns $ALB_ARN \
    --query 'LoadBalancers[0].DNSName' \
    --output text \
    --region $AWS_REGION)

echo "ALB DNS: $ALB_DNS"
```

#### 8.3 Create Target Group

```bash
# Create target group
export TG_ARN=$(aws elbv2 create-target-group \
    --name message-chain-tg \
    --protocol HTTP \
    --port 8080 \
    --vpc-id $VPC_ID \
    --target-type ip \
    --health-check-path /actuator/health \
    --health-check-interval-seconds 30 \
    --health-check-timeout-seconds 5 \
    --healthy-threshold-count 2 \
    --unhealthy-threshold-count 3 \
    --region $AWS_REGION \
    --query 'TargetGroups[0].TargetGroupArn' \
    --output text)
```

#### 8.4 Create ALB Listener

```bash
# Create listener (HTTP on port 80)
aws elbv2 create-listener \
    --load-balancer-arn $ALB_ARN \
    --protocol HTTP \
    --port 80 \
    --default-actions Type=forward,TargetGroupArn=$TG_ARN \
    --region $AWS_REGION
```

### Step 9: Create ECS Service

```bash
# Get subnet IDs as comma-separated string
export SUBNET_LIST=$(echo $SUBNET_IDS | tr ',' ' ')

# Create ECS service
aws ecs create-service \
    --cluster message-chain-cluster \
    --service-name message-chain-service \
    --task-definition message-chain-api-task \
    --desired-count 2 \
    --launch-type FARGATE \
    --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_LIST],securityGroups=[$ECS_SG_ID],assignPublicIp=ENABLED}" \
    --load-balancers "targetGroupArn=$TG_ARN,containerName=message-chain-api,containerPort=8080" \
    --region $AWS_REGION
```

### Step 10: Verify Deployment

Wait a few minutes for the service to start, then test:

```bash
# Check service status
aws ecs describe-services \
    --cluster message-chain-cluster \
    --services message-chain-service \
    --region $AWS_REGION

# Test the API
curl http://$ALB_DNS/api/message?user=john

# Expected response:
# {
#   "message": "Hello john! Welcome to our system. Your account is ready!",
#   "chain": [...]
# }
```

### Step 11: View Logs

```bash
# View CloudWatch logs
aws logs tail /ecs/message-chain-api --follow --region $AWS_REGION
```

## Testing the Deployed API

Once deployed, test all endpoints:

```bash
# Replace with your ALB DNS
export API_URL=http://your-alb-dns-here

# Test main endpoint
curl "$API_URL/api/message?user=alice"

# Test health check
curl "$API_URL/actuator/health"

# Update Service A template
curl -X PUT "$API_URL/api/service-a/message" \
  -H "Content-Type: application/json" \
  -d '{"template": "Hey {user}, welcome!"}'

# Test with updated template
curl "$API_URL/api/message?user=bob"

# Test internal endpoints (if needed)
curl -X POST "$API_URL/internal/service-b/append" \
  -H "Content-Type: application/json" \
  -H "X-Internal-Request: true" \
  -d '{"currentMessage": "Test message"}'
```

## Scaling the Service

```bash
# Update desired count
aws ecs update-service \
    --cluster message-chain-cluster \
    --service message-chain-service \
    --desired-count 4 \
    --region $AWS_REGION
```

## Updating the Application

When you make code changes:

```bash
# 1. Build and push new image
mvn clean package
docker build -t message-chain-api:latest .
docker tag message-chain-api:latest $ECR_REPO:v1.0.1
docker push $ECR_REPO:v1.0.1

# 2. Update task definition with new image tag
# Edit task-definition.json to use :v1.0.1
aws ecs register-task-definition \
    --cli-input-json file://task-definition.json

# 3. Update service to use new task definition
aws ecs update-service \
    --cluster message-chain-cluster \
    --service message-chain-service \
    --task-definition message-chain-api-task \
    --force-new-deployment \
    --region $AWS_REGION
```

## Cleanup

To delete all resources and avoid charges:

```bash
# Delete ECS service
aws ecs update-service \
    --cluster message-chain-cluster \
    --service message-chain-service \
    --desired-count 0 \
    --region $AWS_REGION

aws ecs delete-service \
    --cluster message-chain-cluster \
    --service message-chain-service \
    --force \
    --region $AWS_REGION

# Delete ECS cluster
aws ecs delete-cluster \
    --cluster message-chain-cluster \
    --region $AWS_REGION

# Delete ALB
aws elbv2 delete-listener --listener-arn <LISTENER_ARN>
aws elbv2 delete-target-group --target-group-arn $TG_ARN
aws elbv2 delete-load-balancer --load-balancer-arn $ALB_ARN

# Delete security groups (wait for ALB deletion first)
aws ec2 delete-security-group --group-id $ECS_SG_ID
aws ec2 delete-security-group --group-id $ALB_SG_ID

# Delete ECR repository
aws ecr delete-repository \
    --repository-name message-chain-api \
    --force \
    --region $AWS_REGION

# Delete CloudWatch log group
aws logs delete-log-group \
    --log-group-name /ecs/message-chain-api \
    --region $AWS_REGION
```

## Cost Estimation

Approximate monthly costs (US East 1):
- **ECS Fargate**: ~$30-40/month (2 tasks, 0.5 vCPU, 1GB RAM)
- **Application Load Balancer**: ~$20-25/month
- **Data Transfer**: Variable based on usage
- **CloudWatch Logs**: ~$1-5/month

**Total**: ~$51-70/month for a small production deployment

## Troubleshooting

### Service won't start
```bash
# Check service events
aws ecs describe-services \
    --cluster message-chain-cluster \
    --services message-chain-service \
    --query 'services[0].events[0:5]'

# Check task logs
aws logs tail /ecs/message-chain-api --follow
```

### Health check failures
- Verify the application is listening on port 8080
- Check `/actuator/health` endpoint is accessible
- Verify security group allows traffic on port 8080

### Cannot access ALB
- Ensure ALB security group allows inbound traffic on port 80
- Check that ECS tasks are registered in the target group
- Verify tasks are passing health checks

## Additional Resources

- [AWS ECS Documentation](https://docs.aws.amazon.com/ecs/)
- [AWS Fargate Pricing](https://aws.amazon.com/fargate/pricing/)
- [Spring Boot on AWS](https://spring.io/guides/gs/spring-boot-aws/)
