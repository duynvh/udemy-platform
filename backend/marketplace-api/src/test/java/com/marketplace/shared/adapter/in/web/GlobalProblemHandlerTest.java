package com.marketplace.shared.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalProblemHandlerTest.ProblemProbeController.class)
@Import({GlobalProblemHandlerTest.ProblemProbeController.class, GlobalProblemHandler.class})
class GlobalProblemHandlerTest {

    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    @Autowired
    private MockMvcTester mockMvc;

    @Test
    void handle_validationError_returns400WithFieldErrors() {
        var response = mockMvc.post()
                .uri("/test/validated")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\"}")
                .exchange();

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.errors[0].field").isEqualTo("title");
        assertThat(response).bodyJson().extractingPath("$.errors[0].message").isEqualTo("must not be blank");
    }

    @Test
    void handle_unexpectedException_returns500WithoutStackTrace() {
        var response = mockMvc.get().uri("/test/failure").exchange();

        assertThat(response).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR).hasContentType(PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo("Unexpected error");
        assertThat(response).bodyJson().extractingPath("$.traceId").asString().isNotBlank();
        assertThat(response).bodyText().doesNotContain("IllegalStateException", "secret", "at com.");
    }

    @Test
    void handle_malformedJson_returns400() {
        var response = mockMvc.post()
                .uri("/test/validated")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":")
                .exchange();

        assertThat(response).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(PROBLEM_JSON);
        assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo("Request body is not valid JSON");
    }

    /** Test-only endpoints that trigger each error type. Not scanned outside this test. */
    @RestController
    static class ProblemProbeController {

        record ProbeRequest(@NotBlank String title) {}

        @PostMapping("/test/validated")
        void validated(@Valid @RequestBody ProbeRequest request) {
            // Body is validated by Spring before this method runs.
        }

        @GetMapping("/test/failure")
        void failure() {
            throw new IllegalStateException("database password=secret");
        }
    }
}
