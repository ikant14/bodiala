package com.miirphys.bodiala.search;

import java.time.LocalDate;
import java.util.List;

/**
 * Search-by-hotel-ids request. {@code hotelIds} are the supplier's hotel codes (~50 per request
 * recommended). Dates are ISO ({@code yyyy-MM-dd}).
 */
public record HotelIdsSearchRequest(
        LocalDate arrivalDate,
        LocalDate departureDate,
        String countryCode,
        String city,
        String guestNationality,
        List<Long> hotelIds,
        List<RoomRequest> rooms) {
}
