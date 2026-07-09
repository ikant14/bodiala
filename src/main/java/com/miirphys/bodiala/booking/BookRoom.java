package com.miirphys.bodiala.booking;

import java.util.List;

/** A room to book: the (refreshed) offer fields plus the guests occupying it. */
public record BookRoom(
        String type,
        String bookingKey,
        String adults,
        String children,
        String childrenAges,
        String totalRooms,
        String totalRate,
        List<GuestModel> guests) {
}
