package com.miirphys.bodiala.search;

import java.time.LocalDate;
import java.util.List;

/**
 * Search-by-hotel-ids (findhotelbyid) request. {@code hotelIds} are RezLive HotelCodes (from the
 * Hotel Details master); RezLive recommends ~50 ids per request. Dates are ISO ({@code yyyy-MM-dd}).
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
