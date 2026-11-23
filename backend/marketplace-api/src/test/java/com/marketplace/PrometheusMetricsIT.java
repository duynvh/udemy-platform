package com.marketplace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/** Prometheus can scrape request metrics with histogram buckets (needed for p95 SLOs, architecture §11.3). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class PrometheusMetricsIT {

    @LocalServerPort
    private int serverPort;

    @LocalManagementPort
    private int managementPort;

    private final RestClient restClient = RestClient.create();

    @Test
    void prometheusEndpoint_afterPing_exposesRequestHistogramBuckets() {
        restClient.get().uri("http://localhost:" + serverPort + "/api/v1/ping").retrieve().toBodilessEntity();

        String metrics = restClient
                .get()
                .uri("http://localhost:" + managementPort + "/actuator/prometheus")
                .retrieve()
                .body(String.class);

        assertThat(metrics).contains("http_server_requests_seconds_bucket{").contains("uri=\"/api/v1/ping\"");
    }

    @Test
    void prometheusEndpoint_applicationPort_isNotExposed() {
        int status = restClient
                .get()
                .uri("http://localhost:" + serverPort + "/actuator/prometheus")
                .exchange((request, response) -> response.getStatusCode().value());

        assertThat(status).isEqualTo(404);
    }
}
