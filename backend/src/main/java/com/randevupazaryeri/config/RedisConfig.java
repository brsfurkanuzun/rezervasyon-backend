package com.randevupazaryeri.config;

import org.springframework.context.annotation.Configuration;

/**
 * Redis infrastructure is prepared via docker-compose and spring-data-redis dependency.
 * Auto-configuration is excluded by default so local runs work without Redis.
 * Enable a future "redis" profile when caching/rate-limiting is introduced.
 */
@Configuration
public class RedisConfig {
}
