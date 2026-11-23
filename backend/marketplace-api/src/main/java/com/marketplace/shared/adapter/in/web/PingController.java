package com.marketplace.shared.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Walking-skeleton endpoint: proves the path browser → BFF → backend works and is traced.
 * Business modules replace it as the reference path; keep it as a cheap liveness probe.
 * Example: {@code GET /api/v1/ping} returns {@code {"status":"ok"}}.
 */
@RestController
class PingController {

    record PingResponse(String status) {}

    @GetMapping("/api/v1/ping")
    PingResponse ping() {
        return new PingResponse("ok");
    }
}
