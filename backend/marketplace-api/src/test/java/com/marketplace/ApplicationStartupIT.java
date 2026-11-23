package com.marketplace;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.Node;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.context.ActiveProfiles;

/** The application starts against real PostgreSQL, Kafka, and Valkey, and Flyway migrates the database. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ApplicationStartupIT {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Test
    void contextLoads_allContainers_startsAndMigrates() {
        // Script names include the module folder; the first entry is Flyway's own schema-creation marker.
        assertThat(flyway.info().applied())
                .extracting(info -> info.getScript())
                .contains("shared/V0__baseline.sql", "shared/V1__event_publication.sql");
    }

    @Test
    void migration_baseline_createsSharedSchema() {
        Integer schemaCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.schemata where schema_name = 'shared'", Integer.class);

        assertThat(schemaCount).isEqualTo(1);
    }

    @Test
    void migration_eventRegistry_createsPublicationTableInSharedSchema() {
        Boolean tableExists = jdbcTemplate.queryForObject(
                "select to_regclass('shared.event_publication') is not null", Boolean.class);

        assertThat(tableExists).isTrue();
    }

    @Test
    void valkey_ping_answersPong() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            assertThat(connection.ping()).isEqualTo("PONG");
        }
    }

    @Test
    void kafka_describeCluster_returnsOneBroker() throws Exception {
        try (AdminClient admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            Collection<Node> nodes = admin.describeCluster().nodes().get(30, TimeUnit.SECONDS);

            assertThat(nodes).hasSize(1);
        }
    }
}
