package com.miirphys.bodiala.provider.hotelbeds.dto;

import java.math.BigDecimal;

/**
 * Subset of the Hotelbeds {@code POST /hotel-api/1.0/bookings}, {@code GET /bookings/{ref}} and
 * {@code DELETE /bookings/{ref}} responses — they share the {@code booking} envelope.
 * {@code hotel.totalNet} is a string; {@code hotel.cancellationAmount} (cancel only) is a number.
 */
public record BookingResponse(Booking booking) {

    public record Booking(
            String reference,
            String clientReference,
            String status,
            String cancellationReference,
            String currency,
            Hotel hotel) {
    }

    public record Hotel(
            Integer code,
            String name,
            String totalNet,
            BigDecimal cancellationAmount) {
    }
}
