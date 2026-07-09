package com.miirphys.bodiala.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.miirphys.bodiala.provider.ProviderId;
import com.miirphys.bodiala.provider.SearchProvider;
import com.miirphys.bodiala.provider.SearchProviderRegistry;
import com.miirphys.bodiala.provider.error.UpstreamApiException;
import com.miirphys.bodiala.provider.model.CombinedSearchResult;
import com.miirphys.bodiala.provider.model.SearchResult;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the flatten / tag / partial-failure behaviour of {@link SearchAggregationService},
 * using a mocked {@link SearchProvider} (no Spring context / network). (Cross-supplier fan-out is
 * exercised again once a second provider exists.)
 */
class SearchAggregationServiceTest {

    private final SearchProvider hotelbeds = mock(SearchProvider.class);

    @BeforeEach
    void stubId() {
        when(hotelbeds.id()).thenReturn(ProviderId.HOTELBEDS);
    }

    private SearchAggregationService service() {
        return new SearchAggregationService(new SearchProviderRegistry(List.of(hotelbeds), "hotelbeds"));
    }

    private static DestinationSearchRequest request() {
        return new DestinationSearchRequest(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3),
                "ES", "PMI", "ES", List.of(), List.of(new RoomRequest("R", 2, 0, List.of())));
    }

    private static SearchResult resultWith(String session, String currency, String... hotelNames) {
        List<SearchResult.FoundHotel> hotels = java.util.Arrays.stream(hotelNames)
                .map(n -> new SearchResult.FoundHotel(n + "-id", n, "4", "100", List.of()))
                .toList();
        return new SearchResult(session, currency, "ES", "2026-08-01", "2026-08-03", hotels, null);
    }

    @Test
    void tagsEachHotelWithItsSupplierSessionAndCurrency() {
        when(hotelbeds.isConfigured()).thenReturn(true);
        when(hotelbeds.searchByDestination(any())).thenReturn(resultWith("", "EUR", "Palma Grand", "Palma Bay"));

        CombinedSearchResult combined = service().searchByDestination(request(), null);

        assertThat(combined.hotels()).hasSize(2)
                .allSatisfy(h -> {
                    assertThat(h.provider()).isEqualTo("HOTELBEDS");
                    assertThat(h.currency()).isEqualTo("EUR");
                });
        assertThat(combined.providerStatus()).singleElement()
                .satisfies(s -> {
                    assertThat(s.provider()).isEqualTo("HOTELBEDS");
                    assertThat(s.ok()).isTrue();
                    assertThat(s.hotelCount()).isEqualTo(2);
                });
    }

    @Test
    void supplierFailureIsCapturedInStatusNotThrown() {
        when(hotelbeds.isConfigured()).thenReturn(true);
        when(hotelbeds.searchByDestination(any())).thenThrow(new UpstreamApiException("Hotelbeds 400: bad request"));

        CombinedSearchResult combined = service().searchByDestination(request(), null);

        assertThat(combined.hotels()).isEmpty();
        assertThat(combined.providerStatus()).singleElement()
                .satisfies(s -> {
                    assertThat(s.ok()).isFalse();
                    assertThat(s.error()).contains("Hotelbeds 400");
                });
    }

    @Test
    void failsWith503WhenNoSupplierIsConfigured() {
        when(hotelbeds.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> service().searchByDestination(request(), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No hotel supplier is configured");
    }

    @Test
    void explicitProviderQueriesThatSupplier() {
        when(hotelbeds.isConfigured()).thenReturn(true);
        when(hotelbeds.searchByDestination(any())).thenReturn(resultWith("", "EUR", "Palma Grand"));

        CombinedSearchResult combined = service().searchByDestination(request(), ProviderId.HOTELBEDS);

        assertThat(combined.hotels()).singleElement()
                .satisfies(h -> assertThat(h.provider()).isEqualTo("HOTELBEDS"));
    }
}
