package com.miirphys.bodiala.booking;

/**
 * A room offer echoed from a search result into prebook. The numeric-looking fields are opaque
 * pass-through strings taken verbatim from the search response ({@code BookingKey}, {@code TotalRate},
 * etc.).
 */
public record RoomOffer(
        String type,
        String bookingKey,
        String adults,
        String children,
        String childrenAges,
        String totalRooms,
        String totalRate) {
}
