package com.miirphys.bodiala.search;

import com.miirphys.bodiala.provider.ProviderId;
import com.miirphys.bodiala.provider.SearchProvider;
import com.miirphys.bodiala.provider.SearchProviderRegistry;
import com.miirphys.bodiala.provider.model.CombinedSearchResult;
import com.miirphys.bodiala.provider.model.SearchResult;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
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

    private final SearchProviderRegistry providers;

    public SearchAggregationService(SearchProviderRegistry providers) {
        this.providers = providers;
    }

    public CombinedSearchResult searchByDestination(DestinationSearchRequest request, ProviderId only) {
        return aggregate(only, request.guestNationality(), request.arrivalDate(), request.departureDate(),
                provider -> provider.searchByDestination(request));
    }

    public CombinedSearchResult searchByHotelIds(HotelIdsSearchRequest request, ProviderId only) {
        return aggregate(only, request.guestNationality(), request.arrivalDate(), request.departureDate(),
                provider -> provider.searchByHotelIds(request));
    }

    private CombinedSearchResult aggregate(ProviderId only, String guestNationality,
                                           LocalDate arrival, LocalDate departure,
                                           Function<SearchProvider, SearchResult> call) {
        List<SearchProvider> targets = only != null ? List.of(providers.resolve(only)) : providers.all();
        if (targets.stream().noneMatch(SearchProvider::isConfigured)) {
            throw new IllegalStateException("No hotel supplier is configured for search (need credentials for "
                    + targets.stream().map(p -> p.id().name().toLowerCase()).collect(Collectors.joining(" or "))
                    + ").");
        }

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
                List<CombinedSearchResult.Hotel> mapped = flatten(id, result);
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

    private static List<CombinedSearchResult.Hotel> flatten(String provider, SearchResult result) {
        if (result.hotels() == null) {
            return List.of();
        }
        return result.hotels().stream()
                .map(h -> new CombinedSearchResult.Hotel(provider, result.searchSessionId(), result.currency(),
                        h.id(), h.name(), h.rating(), h.price(), h.roomDetails()))
                .toList();
    }
}
