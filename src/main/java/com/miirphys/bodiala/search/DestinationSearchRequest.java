package com.miirphys.bodiala.search;

import java.time.LocalDate;
import java.util.List;

/**
 * Search-by-destination request in our own clean JSON shape. Dates are ISO ({@code yyyy-MM-dd}).
 *
 * <p>{@code cities} may hold one or several supplier destination codes. A single city uses the supplier
 * {@code destination} availability call (complete coverage). Several cities are searched in one call by
 * resolving their hotel codes from the static-data cache and sending them as {@code hotels.hotel[]} —
 * so multi-city results are limited to hotels already imported into the cache. See
 * {@link com.miirphys.bodiala.search.SearchAggregationService}.
 *
 * @param cities           one or more supplier destination codes (e.g. Hotelbeds {@code "PMI"} = Palma)
 * @param countryCode      2-letter ISO country code (e.g. "ES")
 * @param guestNationality 2-letter nationality code
 * @param hotelRatings     optional star-rating filter (1-5)
 */
public record DestinationSearchRequest(
        LocalDate arrivalDate,
        LocalDate departureDate,
        String countryCode,
        List<String> cities,
        String guestNationality,
        List<Integer> hotelRatings,
        List<RoomRequest> rooms) {
}
