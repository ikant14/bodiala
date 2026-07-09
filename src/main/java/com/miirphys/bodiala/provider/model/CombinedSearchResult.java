package com.miirphys.bodiala.provider.model;

import java.util.List;

/**
 * Aggregated hotel-availability search across every supplier. A single flat {@link #hotels} list
 * concatenates the results from each supplier that was queried; each {@link Hotel} is tagged with the
 * {@code provider} that returned it, plus that supplier's {@code searchSessionId} and {@code currency}
 * — everything the booking chain needs to route a follow-up prebook/book back to the right supplier
 * (via {@code ?provider=}) with the right session. Duplicates across suppliers are intentionally NOT
 * de-duplicated yet (the same hotel simply appears twice); that's a later "merge" step.
 *
 * <p>{@link #providerStatus} reports, per supplier queried, whether it responded and how many hotels
 * it contributed — so a partial failure (one supplier down or not configured) is visible in the
 * response instead of blanking the whole search.
 */
public record CombinedSearchResult(
        String guestNationality,
        String arrivalDate,
        String departureDate,
        List<Hotel> hotels,
        List<ProviderStatus> providerStatus) {

    /** One availability result, tagged with the supplier (and its session/currency) that produced it. */
    public record Hotel(
            String provider,
            String searchSessionId,
            String currency,
            String id,
            String name,
            String rating,
            String price,
            List<SearchResult.RoomDetail> roomDetails) {
    }

    /** Per-supplier outcome for one aggregated search. */
    public record ProviderStatus(
            String provider,
            boolean ok,
            String error,
            int hotelCount) {
    }
}
