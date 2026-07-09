package com.miirphys.bodiala.search;

import java.time.LocalDate;
import java.util.List;

/**
 * Search-by-destination request in our own clean JSON shape. Dates are ISO ({@code yyyy-MM-dd}).
 *
 * @param city             the supplier's destination code (e.g. Hotelbeds {@code "PMI"} = Palma)
 * @param countryCode      2-letter ISO country code (e.g. "ES")
 * @param guestNationality 2-letter nationality code
 * @param hotelRatings     optional star-rating filter (1-5)
 */
public record DestinationSearchRequest(
        LocalDate arrivalDate,
        LocalDate departureDate,
        String countryCode,
        String city,
        String guestNationality,
        List<Integer> hotelRatings,
        List<RoomRequest> rooms) {
}
