package com.miirphys.bodiala.booking;

/** A room for a pre-booking cancellation-policy lookup (keyed on BookingKey). */
public record PolicyRoom(
        String bookingKey,
        String adults,
        String children,
        String childrenAges,
        String type) {
}
