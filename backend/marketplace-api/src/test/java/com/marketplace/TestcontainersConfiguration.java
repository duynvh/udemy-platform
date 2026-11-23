package com.marketplace;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Real infrastructure for integration tests: the same images and versions as infra/compose.yaml.
 * Spring caches the test context, so all integration tests with this configuration share the containers.
 * Example: {@code @SpringBootTest @Import(TestcontainersConfiguration.class) @ActiveProfiles("test")}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final DockerImageName POSTGRES_IMAGE =
            DockerImageName.parse("docker.io/pgvector/pgvector:0.8.7-pg18").asCompatibleSubstituteFor("postgres");
    // Fully qualified names (needed for Podman) must be declared compatible with the short names Testcontainers knows.
    private static final DockerImageName KAFKA_IMAGE =
            DockerImageName.parse("docker.io/apache/kafka:4.1.1").asCompatibleSubstituteFor("apache/kafka");
    private static final DockerImageName VALKEY_IMAGE = DockerImageName.parse("docker.io/valkey/valkey:8.1.12-alpine");
    private static final int VALKEY_PORT = 6379;

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE).withDatabaseName("marketplace");
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(KAFKA_IMAGE);
    }

    // Valkey speaks the Redis protocol; name = "redis" selects Spring's Redis connection details.
    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> valkeyContainer() {
        return new GenericContainer<>(VALKEY_IMAGE).withExposedPorts(VALKEY_PORT);
    }
}
