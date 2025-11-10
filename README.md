# Message Chain API

A distributed message processing demo using Spring Boot that demonstrates the **Chain of Responsibility pattern** across three service endpoints. Each service appends its message contribution, and the final result flows back through the chain to the client.

## Architecture

```
Client → Service A → Service B → Service C
          ↓          ↓          ↓
       "Hello!"  "Welcome!" "Ready!"
          ↓          ↓          ↓
Client ← Complete Message Chain ←
```

### Flow

1. **Client** calls Service A with a username
2. **Service A** generates a greeting and calls Service B
3. **Service B** appends a welcome message and calls Service C
4. **Service C** adds the final confirmation
5. **Response** flows back: C → B → A → Client

## Tech Stack

- **Java 21**
- **Spring Boot 3.5.7**
- **Maven** for build management
- **Docker** for containerization
- **AWS ECS** for deployment (optional)

## Features

- Chain of Responsibility pattern implementation
- RESTful APIs for each service
- In-memory message template storage
- Runtime template updates
- Health checks for container orchestration
- Comprehensive error handling
- Request/response logging
- Docker support with multi-stage builds

## Project Structure

```
src/main/java/org/example/microservice_demo/
├── config/
│   └── MessageTemplateConfig.java      # Message template configuration
├── controller/
│   ├── ServiceAController.java         # Client-facing API
│   ├── ServiceBController.java         # Internal API
│   └── ServiceCController.java         # Final internal API
├── exception/
│   ├── GlobalExceptionHandler.java     # Global error handling
│   └── ServiceChainException.java      # Custom exception
├── model/
│   ├── ChainLink.java                  # Chain contribution DTO
│   ├── ErrorResponse.java              # Error response DTO
│   ├── MessageResponse.java            # Main response DTO
│   ├── ServiceBRequest.java            # Service B request DTO
│   ├── ServiceBResponse.java           # Service B response DTO
│   ├── ServiceCRequest.java            # Service C request DTO
│   ├── ServiceCResponse.java           # Service C response DTO
│   ├── UpdateTemplateRequest.java      # Template update request DTO
│   └── UpdateTemplateResponse.java     # Template update response DTO
├── service/
│   └── MessageService.java             # Business logic
└── MicroserviceDemoApplication.java    # Main application
```

## Quick Start

### Prerequisites

- Java 21
- Maven 3.9+
- Docker (optional)

### Run Locally with Maven

```bash
# Clone the repository
git clone https://github.com/whdecx/microservice-demo.git
cd microservice-demo

# Build the project
mvn clean package

# Run the application
java -jar target/Microservice_demo-0.0.1-SNAPSHOT.jar

# Or run directly with Maven
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Run with Docker

#### Option 1: Build from Local Source (Default)

```bash
# Clone the repository first
git clone https://github.com/whdecx/microservice-demo.git
cd microservice-demo

# Build the Docker image from local files
docker build -t message-chain-api:latest .

# Run the container
docker run -p 8080:8080 message-chain-api:latest
```

#### Option 2: Build Directly from GitHub

```bash
# Clone the repository first (for the Dockerfile)
git clone https://github.com/whdecx/microservice-demo.git
cd microservice-demo

# Build the Docker image pulling source from GitHub main branch
docker build --build-arg BUILD_SOURCE=github -t message-chain-api:latest .

# Build from a specific branch
docker build \
  --build-arg BUILD_SOURCE=github \
  --build-arg GIT_BRANCH=develop \
  -t message-chain-api:develop .

# Build from a different repository fork
docker build \
  --build-arg BUILD_SOURCE=github \
  --build-arg GIT_REPO=https://github.com/yourfork/microservice-demo.git \
  --build-arg GIT_BRANCH=feature-branch \
  -t message-chain-api:custom .

# Run the container
docker run -p 8080:8080 message-chain-api:latest
```

**Benefits of GitHub build:**
- Always builds from the latest code in the repository
- Useful for CI/CD pipelines
- No need to keep local source files in sync
- Can build specific branches or forks

### Run with Docker Compose

```bash
# Start the service
docker-compose up -d

# View logs
docker-compose logs -f

# Stop the service
docker-compose down
```

## API Documentation

### 1. Get Complete Message (Client-Facing)

**Endpoint**: `GET /api/message`

**Description**: Main entry point that initiates the message chain.

**Query Parameters**:
- `user` (optional, default: "guest"): Username to personalize the message (max 50 characters)

**Example Request**:
```bash
curl "http://localhost:8080/api/message?user=john"
```

**Example Response**:
```json
{
  "message": "Hello john! Welcome to our system. Your account is ready!",
  "chain": [
    {
      "service": "service-a",
      "contribution": "Hello john!",
      "timestamp": "2024-11-09T14:30:00.100Z"
    },
    {
      "service": "service-b",
      "contribution": "Welcome to our system.",
      "timestamp": "2024-11-09T14:30:00.250Z"
    },
    {
      "service": "service-c",
      "contribution": "Your account is ready!",
      "timestamp": "2024-11-09T14:30:00.400Z"
    }
  ],
  "complete": true,
  "totalLength": 58,
  "processingTimeMs": 350
}
```

### 2. Service B Internal API

**Endpoint**: `POST /internal/service-b/append`

**Description**: Internal endpoint called by Service A.

**Headers**:
- `Content-Type: application/json`
- `X-Internal-Request: true` (optional)

**Request Body**:
```json
{
  "currentMessage": "Hello john!"
}
```

**Example Request**:
```bash
curl -X POST "http://localhost:8080/internal/service-b/append" \
  -H "Content-Type: application/json" \
  -H "X-Internal-Request: true" \
  -d '{"currentMessage": "Hello john!"}'
```

**Example Response**:
```json
{
  "message": "Hello john! Welcome to our system. Your account is ready!",
  "chain": [
    {
      "service": "service-b",
      "contribution": "Welcome to our system.",
      "timestamp": "2024-11-09T14:30:00.250Z"
    },
    {
      "service": "service-c",
      "contribution": "Your account is ready!",
      "timestamp": "2024-11-09T14:30:00.400Z"
    }
  ]
}
```

### 3. Service C Internal API

**Endpoint**: `POST /internal/service-c/finalize`

**Description**: Internal endpoint called by Service B.

**Headers**:
- `Content-Type: application/json`
- `X-Internal-Request: true` (optional)

**Request Body**:
```json
{
  "currentMessage": "Hello john! Welcome to our system."
}
```

**Example Request**:
```bash
curl -X POST "http://localhost:8080/internal/service-c/finalize" \
  -H "Content-Type: application/json" \
  -H "X-Internal-Request: true" \
  -d '{"currentMessage": "Hello john! Welcome to our system."}'
```

**Example Response**:
```json
{
  "message": "Hello john! Welcome to our system. Your account is ready!",
  "contribution": "Your account is ready!",
  "timestamp": "2024-11-09T14:30:00.400Z"
}
```

### 4. Update Message Templates

You can update the message template for each service at runtime.

#### Update Service A Template

**Endpoint**: `PUT /api/service-a/message`

**Example**:
```bash
curl -X PUT "http://localhost:8080/api/service-a/message" \
  -H "Content-Type: application/json" \
  -d '{"template": "Hey {user}, nice to see you!"}'
```

**Response**:
```json
{
  "service": "service-a",
  "template": "Hey {user}, nice to see you!",
  "updatedAt": "2024-11-09T14:35:00Z",
  "message": "Message template updated successfully"
}
```

#### Update Service B Template

**Endpoint**: `PUT /api/service-b/message`

**Example**:
```bash
curl -X PUT "http://localhost:8080/api/service-b/message" \
  -H "Content-Type: application/json" \
  -d '{"template": "{previous_message} Everything is ready for you."}'
```

#### Update Service C Template

**Endpoint**: `PUT /api/service-c/message`

**Example**:
```bash
curl -X PUT "http://localhost:8080/api/service-c/message" \
  -H "Content-Type: application/json" \
  -d '{"template": "{previous_message} Let'\''s get started!"}'
```

### 5. Health Check

**Endpoint**: `GET /actuator/health`

**Example**:
```bash
curl "http://localhost:8080/actuator/health"
```

**Response**:
```json
{
  "status": "UP"
}
```

## Configuration

Message templates are configured in `src/main/resources/application.yml`:

```yaml
message:
  service-a:
    template: "Hello {user}!"
    service-name: "service-a"
    description: "Greeting service"

  service-b:
    template: "{previous_message} Welcome to our system."
    service-name: "service-b"
    description: "Welcome message service"

  service-c:
    template: "{previous_message} Your account is ready!"
    service-name: "service-c"
    description: "Confirmation service"
```

### Template Variables

- `{user}`: Replaced with the username from the request (Service A only)
- `{previous_message}`: Replaced with the message from previous services (Service B and C)

## Error Handling

The API returns structured error responses:

### Validation Error (400)
```json
{
  "error": "invalid_input",
  "message": "Query parameter 'user' must not exceed 50 characters"
}
```

### Service Chain Failure (500)
```json
{
  "error": "chain_failed",
  "message": "Failed to complete message chain",
  "failedService": "service-b",
  "details": "Connection timeout to internal service",
  "partialMessage": "Hello john!",
  "retryAfter": 30
}
```

### Service Unavailable (503)
```json
{
  "error": "service_unavailable",
  "message": "Service is temporarily unavailable",
  "retryAfter": 30
}
```

## Testing

### Run Unit Tests

```bash
mvn test
```

### Manual Testing

Test the complete flow:

```bash
# Test with default user
curl "http://localhost:8080/api/message"

# Test with custom user
curl "http://localhost:8080/api/message?user=alice"

# Update Service A template
curl -X PUT "http://localhost:8080/api/service-a/message" \
  -H "Content-Type: application/json" \
  -d '{"template": "Greetings {user}!"}'

# Test with updated template
curl "http://localhost:8080/api/message?user=bob"

# Test validation (should fail)
curl "http://localhost:8080/api/message?user=$(python3 -c 'print("a"*51)')"

# Check health
curl "http://localhost:8080/actuator/health"
```

## Monitoring

### Application Logs

```bash
# View logs in Docker
docker-compose logs -f

# View logs in local run
# Logs are printed to console
```

### Metrics

Access Spring Boot Actuator metrics:

```bash
curl "http://localhost:8080/actuator/metrics"
```

## AWS Deployment

For detailed AWS ECS deployment instructions, see [AWS-DEPLOYMENT.md](./AWS-DEPLOYMENT.md)

Quick overview:
1. Build and push Docker image to ECR
2. Create ECS cluster (Fargate)
3. Configure Application Load Balancer
4. Create and deploy ECS service
5. Test via ALB endpoint

## Development

### Adding New Features

1. **Add new service to chain**:
   - Create controller in `controller/` package
   - Add logic to `MessageService`
   - Update configuration in `application.yml`

2. **Modify message templates**:
   - Update `application.yml` for permanent changes
   - Use PUT endpoints for runtime changes

3. **Add validation**:
   - Use Jakarta validation annotations in model classes
   - Errors are automatically handled by `GlobalExceptionHandler`

### Building for Production

#### Build from Local Source

```bash
# Build optimized JAR
mvn clean package -DskipTests

# Build Docker image from local files
docker build -t message-chain-api:v1.0.0 .

# Push to container registry
docker tag message-chain-api:v1.0.0 your-registry/message-chain-api:v1.0.0
docker push your-registry/message-chain-api:v1.0.0
```

#### Build from GitHub (Recommended for CI/CD)

```bash
# Build Docker image directly from GitHub main branch
docker build \
  --build-arg BUILD_SOURCE=github \
  -t message-chain-api:v1.0.0 \
  https://github.com/whdecx/microservice-demo.git#main

# Build from a specific release tag
docker build \
  --build-arg BUILD_SOURCE=github \
  --build-arg GIT_BRANCH=v1.0.0 \
  -t message-chain-api:v1.0.0 \
  https://github.com/whdecx/microservice-demo.git#v1.0.0

# Push to container registry
docker tag message-chain-api:v1.0.0 your-registry/message-chain-api:v1.0.0
docker push your-registry/message-chain-api:v1.0.0
```

#### CI/CD Pipeline Example

```yaml
# Example GitHub Actions workflow
- name: Build Docker image from GitHub
  run: |
    docker build \
      --build-arg BUILD_SOURCE=github \
      --build-arg GIT_BRANCH=${{ github.ref_name }} \
      -t ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }} \
      .
```

## Design Patterns Used

1. **Chain of Responsibility**: Services call each other in sequence
2. **Builder Pattern**: Used in model classes with Lombok
3. **Dependency Injection**: Spring's IoC container
4. **Template Method**: Message template processing
5. **Strategy Pattern**: Configurable message templates

## Performance Considerations

- In-memory storage (fast but not persistent)
- Synchronous processing (suitable for demo)
- Single JVM process (simple deployment)
- Stateless design (horizontally scalable)

For production systems, consider:
- Async processing with message queues
- Distributed tracing (e.g., Spring Cloud Sleuth)
- Caching layer
- Database for persistent storage

## Troubleshooting

### Port 8080 already in use
```bash
# Find and kill the process
lsof -i :8080
kill -9 <PID>

# Or run on different port
java -jar target/Microservice_demo-0.0.1-SNAPSHOT.jar --server.port=8081
```

### Build fails
```bash
# Clean Maven cache
mvn clean

# Rebuild with dependencies
mvn clean install -U
```

### Docker build fails
```bash
# Clear Docker cache
docker builder prune

# Rebuild without cache
docker build --no-cache -t message-chain-api:latest .
```

## License

MIT License - See LICENSE file for details

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## Contact

For questions or issues, please open an issue on GitHub.

---

Built with Spring Boot and deployed on AWS ECS
