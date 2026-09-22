package com.randevupazaryeri.support;

import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Integration tests use the docker-compose Postgres on host port 5433
 * (local 5432 is often already occupied).
 */
public final class PostgresTestSupport {

    private PostgresTestSupport() {
    }

    public static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5433/randevu");
        registry.add("spring.datasource.username", () -> "randevu");
        registry.add("spring.datasource.password", () -> "randevu");
        registry.add("app.jwt.secret", () -> "test-secret-key-must-be-long-enough-for-hs256-algorithm!!");
        registry.add("app.seed.enabled", () -> "false");
    }
}
