package com.miirphys.bodiala.search;

import java.time.LocalDate;
import java.util.List;

/**
 * Search-by-destination (findhotel) request in our own clean JSON shape. Dates are ISO
 * ({@code yyyy-MM-dd}); the service converts them to RezLive's {@code dd/MM/yyyy}.
 *
 * @param city             RezLive numeric city code (e.g. "968" = Dubai), from the City master
 * @param countryCode      2-letter ISO country code (e.g. "AE")
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
