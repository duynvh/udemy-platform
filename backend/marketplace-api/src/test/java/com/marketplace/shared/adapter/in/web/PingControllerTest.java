package com.marketplace.shared.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(PingController.class)
class PingControllerTest {

    @Autowired
    private MockMvcTester mockMvc;

    @Test
    void ping_anyRequest_returnsOk() {
        var response = mockMvc.get().uri("/api/v1/ping").exchange();

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().extractingPath("$.status").isEqualTo("ok");
    }
}
