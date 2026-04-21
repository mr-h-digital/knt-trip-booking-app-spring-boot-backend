package com.kntransport.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.hikari.maximum-pool-size=2",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=test-secret-that-is-long-enough-for-hmac-sha256-algorithm-yes",
    "app.jwt.expiration-ms=86400000"
})
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
