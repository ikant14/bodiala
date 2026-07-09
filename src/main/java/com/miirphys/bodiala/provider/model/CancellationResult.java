package com.miirphys.bodiala.provider.model;

/**
 * Provider-neutral result of cancelling a booking. JSON shape matches what the frontend consumed
 * from RezLive's {@code CancellationResponse}. {@code status} is the supplier's success flag
 * (RezLive {@code "true"} / Hotelbeds {@code "CANCELLED"}); {@code cancellationCharges} is the
 * charge applied (may be {@code "0.00"} for a free cancellation).
 */
public record CancellationResult(
        String bookingId,
        String bookingCode,
        String status,
        String cancellationCharges,
        String currency) {
}
