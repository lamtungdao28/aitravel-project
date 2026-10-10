package com.travelhub;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.security.test.context.support.WithMockUser(username="test-admin", authorities={"ROLE_ADMIN","DESTINATION_READ","DESTINATION_CREATE","DESTINATION_UPDATE","DESTINATION_DELETE","PLACE_READ","PLACE_CREATE","PLACE_UPDATE","PLACE_DELETE","REVIEW_CREATE","REVIEW_UPDATE","REVIEW_DELETE","BOOKING_READ","BOOKING_CREATE","BOOKING_UPDATE","BOOKING_DELETE","USER_READ","USER_UPDATE","USER_DELETE","ADMIN_DASHBOARD_READ"})
public abstract class PostgresIntegrationTest {
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    static { POSTGRES.start(); REDIS.start(); }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
