package com.marketplace.shared.adapter.in.web;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns framework and unexpected errors into RFC 9457 Problem Details.
 * Lowest precedence: each module adds its own advice for its domain exceptions, and that advice wins.
 * Example: an invalid body returns 400 with {@code "errors":[{"field":"title","message":"must not be blank"}]}.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class GlobalProblemHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalProblemHandler.class);
    private static final String UNEXPECTED_ERROR_DETAIL = "Unexpected error";
    private static final String MALFORMED_BODY_DETAIL = "Request body is not valid JSON";
    // Micrometer Tracing writes the current trace id to this MDC key (T11).
    private static final String TRACE_ID_MDC_KEY = "traceId";

    record FieldErrorView(String field, String message) {}

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ex.getBody();
        problem.setProperty("errors", toFieldErrorViews(ex));
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // The parser message can echo request content; never return it to the client.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, MALFORMED_BODY_DETAIL);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        String traceId = currentTraceId();
        log.error("Unexpected error, traceId={}", traceId, ex);
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR_DETAIL);
        problem.setProperty("traceId", traceId);
        return problem;
    }

    private static List<FieldErrorView> toFieldErrorViews(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorView(error.getField(), error.getDefaultMessage()))
                .toList();
    }

    /** Trace id from tracing when present; otherwise a random id that still links response and log line. */
    private static String currentTraceId() {
        String tracingId = MDC.get(TRACE_ID_MDC_KEY);
        if (tracingId != null) {
            return tracingId;
        }
        return UUID.randomUUID().toString();
    }
}
