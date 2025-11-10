package org.example.microservicedemo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous execution using @Async annotation
 * Provides a custom thread pool for RestClient async operations
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    /**
     * Custom executor for async RestClient calls
     *
     * Thread pool configuration:
     * - Core pool size: 5 threads (always maintained)
     * - Max pool size: 10 threads (scales up under load)
     * - Queue capacity: 25 tasks (queued when all threads busy)
     *
     * @return Configured ThreadPoolTaskExecutor
     */
    @Bean(name = "asyncRestClientExecutor")
    public Executor asyncRestClientExecutor() {
        log.info("Initializing async RestClient executor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size - threads always kept alive
        executor.setCorePoolSize(5);

        // Maximum pool size - max threads that can be created
        executor.setMaxPoolSize(10);

        // Queue capacity - number of tasks to queue before rejecting
        executor.setQueueCapacity(25);

        // Thread name prefix for easier debugging
        executor.setThreadNamePrefix("AsyncRestClient-");

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // Timeout for waiting on shutdown (seconds)
        executor.setAwaitTerminationSeconds(30);

        // Initialize the executor
        executor.initialize();

        log.info("Async RestClient executor initialized with corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }
}
