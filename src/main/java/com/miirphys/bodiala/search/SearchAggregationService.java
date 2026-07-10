package com.miirphys.bodiala.search;

import com.miirphys.bodiala.provider.ProviderId;
import com.miirphys.bodiala.provider.SearchProvider;
import com.miirphys.bodiala.provider.SearchProviderRegistry;
import com.miirphys.bodiala.provider.model.CombinedSearchResult;
import com.miirphys.bodiala.provider.model.SearchResult;
import com.miirphys.bodiala.staticdata.repo.HotelRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Fans a search out across suppliers and flattens the results into one provider-tagged
 * {@link CombinedSearchResult}. By default it queries <em>every</em> configured supplier so the caller
 * sees all results; passing an explicit {@code provider} narrows it to that one.
 *
 * <p>Partial-failure tolerant: a supplier with no credentials is reported as {@code not configured}
 * and skipped, and a supplier that throws (upstream 4xx/5xx, transport error) has its message captured
 * in {@link CombinedSearchResult#providerStatus()} — neither blanks the other supplier's hotels. Only
 * when <em>no</em> queried supplier is configured does the whole search fail (503, via
 * {@link IllegalStateException}), preserving the "credentials not set" contract.
 */
@Service
public class SearchAggregationService {

    private static final Logger log = LoggerFactory.getLogger(SearchAggregationService.class);

    private static final Pattern LEADING_DIGITS = Pattern.compile("(\\d+)");

    /** Hotelbeds caps a by-hotel-ids availability request at 2000 codes. */
    private static final int MAX_HOTEL_IDS = 2000;

    private final SearchProviderRegistry providers;
    private final HotelRepository hotels;

    public SearchAggregationService(SearchProviderRegistry providers, HotelRepository hotels) {
        this.providers = providers;
        this.hotels = hotels;
    }

    /**
     * Destination search over one or several cities. A single city uses the supplier {@code destination}
     * call (complete coverage). Several cities are searched in one call by resolving their hotel codes
     * from the static-data cache and querying by hotel ids — so multi-city results only include hotels
     * already imported into the cache. The star filter applies to either route.
     */
    public CombinedSearchResult searchByDestination(DestinationSearchRequest request, ProviderId only) {
        // Resolve targets first so "no supplier configured" (503) wins over request validation (400),
        // matching the credentials-not-set contract.
        List<SearchProvider> targets = targetsOrThrow(only);
        List<String> cities = request.cities() == null ? List.of()
                : request.cities().stream()
                        .filter(c -> c != null && !c.isBlank())
                        .map(String::trim)
                        .distinct()
                        .toList();
        if (cities.isEmpty()) {
            throw new IllegalArgumentException("at least one city is required for destination search");
        }

        String gn = request.guestNationality();
        LocalDate arrival = request.arrivalDate();
        LocalDate departure = request.departureDate();

        if (cities.size() == 1) {
            DestinationSearchRequest single = new DestinationSearchRequest(arrival, departure,
                    request.countryCode(), cities, gn, request.hotelRatings(), request.rooms());
            return aggregate(targets, gn, arrival, departure, request.hotelRatings(),
                    provider -> provider.searchByDestination(single));
        }

        // Multi-city: one availability call over every selected city's cached hotel codes.
        List<Long> codes = hotels.findHotelCodesByCityCodeIn(cities);
        if (codes.size() > MAX_HOTEL_IDS) {
            log.warn("Multi-city search resolved {} cached hotels across {} cities; capping at {}.",
                    codes.size(), cities.size(), MAX_HOTEL_IDS);
            codes = codes.subList(0, MAX_HOTEL_IDS);
        }
        Function<SearchProvider, SearchResult> call;
        if (codes.isEmpty()) {
            // None of the selected cities have cached hotels — an empty result, not a supplier failure.
            log.info("Multi-city search over {} found no cached hotels.", cities);
            call = provider -> emptyResult(gn, arrival, departure);
        } else {
            HotelIdsSearchRequest ids = new HotelIdsSearchRequest(arrival, departure, request.countryCode(),
                    null, gn, codes, request.rooms());
            call = provider -> provider.searchByHotelIds(ids);
        }
        return aggregate(targets, gn, arrival, departure, request.hotelRatings(), call);
    }

    public CombinedSearchResult searchByHotelIds(HotelIdsSearchRequest request, ProviderId only) {
        // No star filter for by-hotel-ids: the caller already named the exact hotels.
        return aggregate(targetsOrThrow(only), request.guestNationality(), request.arrivalDate(),
                request.departureDate(), null, provider -> provider.searchByHotelIds(request));
    }

    /** The suppliers a search will hit; 503 if none of them is configured (credentials-not-set contract). */
    private List<SearchProvider> targetsOrThrow(ProviderId only) {
        List<SearchProvider> targets = only != null ? List.of(providers.resolve(only)) : providers.all();
        if (targets.stream().noneMatch(SearchProvider::isConfigured)) {
            throw new IllegalStateException("No hotel supplier is configured for search (need credentials for "
                    + targets.stream().map(p -> p.id().name().toLowerCase()).collect(Collectors.joining(" or "))
                    + ").");
        }
        return targets;
    }

    private CombinedSearchResult aggregate(List<SearchProvider> targets, String guestNationality,
                                           LocalDate arrival, LocalDate departure,
                                           List<Integer> ratings,
                                           Function<SearchProvider, SearchResult> call) {
        // Empty/absent ⇒ no star filter. The chips allow a non-contiguous set (e.g. {3,5}), so we filter
        // by exact membership here rather than a min/max range on the supplier request.
        Set<Integer> wantedStars = ratings == null ? Set.of() : Set.copyOf(ratings);

        List<CombinedSearchResult.Hotel> hotels = new ArrayList<>();
        List<CombinedSearchResult.ProviderStatus> statuses = new ArrayList<>();
        for (SearchProvider provider : targets) {
            String id = provider.id().name();
            if (!provider.isConfigured()) {
                statuses.add(new CombinedSearchResult.ProviderStatus(id, false, "not configured", 0));
                continue;
            }
            try {
                SearchResult result = call.apply(provider);
                List<CombinedSearchResult.Hotel> mapped = flatten(id, result, wantedStars);
                hotels.addAll(mapped);
                statuses.add(new CombinedSearchResult.ProviderStatus(id, result.error() == null,
                        result.error(), mapped.size()));
            } catch (RuntimeException e) {
                log.warn("Search against {} failed (aggregated search continues): {}", id, e.getMessage());
                statuses.add(new CombinedSearchResult.ProviderStatus(id, false, e.getMessage(), 0));
            }
        }
        return new CombinedSearchResult(guestNationality,
                arrival == null ? null : arrival.toString(),
                departure == null ? null : departure.toString(),
                hotels, statuses);
    }

    private static List<CombinedSearchResult.Hotel> flatten(String provider, SearchResult result,
                                                            Set<Integer> wantedStars) {
        if (result.hotels() == null) {
            return List.of();
        }
        return result.hotels().stream()
                .filter(h -> ratingMatches(h.rating(), wantedStars))
                .map(h -> new CombinedSearchResult.Hotel(provider, result.searchSessionId(), result.currency(),
                        h.id(), h.name(), h.city(), h.rating(), h.price(), h.roomDetails()))
                .toList();
    }

    /** An empty (but well-formed) result used when a multi-city search resolves to no cached hotels. */
    private static SearchResult emptyResult(String guestNationality, LocalDate arrival, LocalDate departure) {
        return new SearchResult("", null, guestNationality,
                arrival == null ? null : arrival.toString(),
                departure == null ? null : departure.toString(),
                List.of(), null);
    }

    /**
     * True when no star filter is active, or the hotel's rating resolves to one of the wanted stars. A
     * hotel whose rating is missing/unparseable is excluded while a filter is active (we can't confirm a
     * match). Parses the leading digits so it works whether the provider emits {@code "4"} or {@code "4EST"}.
     */
    private static boolean ratingMatches(String rating, Set<Integer> wantedStars) {
        if (wantedStars.isEmpty()) {
            return true;
        }
        if (rating == null) {
            return false;
        }
        Matcher m = LEADING_DIGITS.matcher(rating);
        return m.find() && wantedStars.contains(Integer.valueOf(m.group(1)));
    }
}
