package com.miirphys.bodiala.web;

import com.miirphys.bodiala.provider.error.UpstreamApiException;
import com.miirphys.bodiala.provider.error.UpstreamTransportException;
import java.util.NoSuchElementException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

/**
 * Central mapping of service-layer exceptions to HTTP responses, so controllers don't each
 * repeat the same try/catch. Returns RFC-7807 {@link ProblemDetail} bodies.
 *
 * <p>{@code ResponseStatusException}s thrown directly by controllers keep their own status and
 * are handled by Spring's default handler, not here.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Bad input (validation) → 400. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Not usable yet (e.g. credentials not configured) → 503. */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail serviceUnavailable(IllegalStateException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    /** Unknown resource (e.g. bookingId not found) → 404. */
    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail notFound(NoSuchElementException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** Duplicate key (e.g. a booking id already stored) → 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflict(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Conflict persisting the record (duplicate key?)");
    }

    /** Operation the active provider does not implement yet (e.g. a Hotelbeds secondary read) → 501. */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ProblemDetail notImplemented(UnsupportedOperationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_IMPLEMENTED, e.getMessage());
    }

    /** Supplier returned an error body (Hotelbeds error JSON) → 502. */
    @ExceptionHandler(UpstreamApiException.class)
    public ProblemDetail upstreamError(UpstreamApiException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Upstream provider error: " + e.getMessage());
    }

    /** Transport failure talking to a supplier (timeout, connection refused, non-2xx) → 502. */
    @ExceptionHandler(RestClientException.class)
    public ProblemDetail transportError(RestClientException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Upstream request failed: " + e.getMessage());
    }

    /** Unparseable/corrupt supplier response (bad XML/JSON, gzip error, marshalling failure) → 502. */
    @ExceptionHandler(UpstreamTransportException.class)
    public ProblemDetail codecError(UpstreamTransportException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Upstream response could not be processed: " + e.getMessage());
    }
}
