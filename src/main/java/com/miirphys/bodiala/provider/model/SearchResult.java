package com.miirphys.bodiala.provider.model;

import java.util.List;

/**
 * Provider-neutral hotel-availability search result — the shape the frontend consumes. The
 * Hotelbeds provider maps its native availability response into this record, and the controller
 * returns it.
 *
 * <p>{@code searchSessionId} is empty for stateless suppliers (Hotelbeds); the per-room offer token
 * lives in {@link RoomDetail#bookingKey} (for Hotelbeds it is {@code rateKey#rateType}).
 */
public record SearchResult(
        String searchSessionId,
        String currency,
        String guestNationality,
        String arrivalDate,
        String departureDate,
        List<FoundHotel> hotels,
        String error) {

    public record FoundHotel(
            String id,
            String name,
            String rating,
            String price,
            List<RoomDetail> roomDetails) {
    }

    public record RoomDetail(
            String type,
            String bookingKey,
            String adults,
            String children,
            String childrenAges,
            String totalRooms,
            String totalRate,
            String roomDescription,
            String boardBasis) {
    }
}
