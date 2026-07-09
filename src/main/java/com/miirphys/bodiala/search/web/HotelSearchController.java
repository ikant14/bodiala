package com.miirphys.bodiala.search.web;

import com.miirphys.bodiala.provider.ProviderId;
import com.miirphys.bodiala.provider.model.CombinedSearchResult;
import com.miirphys.bodiala.search.DestinationSearchRequest;
import com.miirphys.bodiala.search.HotelIdsSearchRequest;
import com.miirphys.bodiala.search.SearchAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Live hotel availability search. Fans out across every configured supplier and returns one combined,
 * provider-tagged {@link CombinedSearchResult}; the optional {@code ?provider=} query param
 * (hotelbeds, case-insensitive) narrows it to a single supplier. Each returned hotel carries its
 * supplier + that supplier's session/currency, so a follow-up prebook/book can route correctly. A
 * supplier that's unconfigured or fails is reported in {@code providerStatus} rather than blanking the
 * search; only when no queried supplier is configured does the call 503.
 */
@RestController
@RequestMapping("/api/search")
@Tag(name = "Hotel search (live)", description = "Aggregated availability search; narrow with ?provider=")
public class HotelSearchController {

    private static final String PROVIDER_DESC =
            "Limit the search to one supplier: hotelbeds (case-insensitive). Omit to query all configured suppliers.";

    private final SearchAggregationService searchAggregator;

    public HotelSearchController(SearchAggregationService searchAggregator) {
        this.searchAggregator = searchAggregator;
    }

    @Operation(summary = "Search hotels by destination",
            description = "Availability for a destination over a date range, aggregated across suppliers. "
                    + "Dates are ISO yyyy-MM-dd; `city` is the supplier's destination code.")
    @PostMapping("/by-destination")
    public CombinedSearchResult byDestination(@RequestBody DestinationSearchRequest request,
                                              @Parameter(description = PROVIDER_DESC)
                                              @RequestParam(required = false) ProviderId provider) {
        return searchAggregator.searchByDestination(request, provider);
    }

    @Operation(summary = "Search hotels by hotel ids",
            description = "Availability for specific supplier hotel codes (~50 per request recommended), "
                    + "aggregated across suppliers.")
    @PostMapping("/by-hotel-ids")
    public CombinedSearchResult byHotelIds(@RequestBody HotelIdsSearchRequest request,
                                           @Parameter(description = PROVIDER_DESC)
                                           @RequestParam(required = false) ProviderId provider) {
        return searchAggregator.searchByHotelIds(request, provider);
    }
}
