package com.miirphys.bodiala.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
 * Unit tests for the fan-out / flatten / partial-failure behaviour of {@link SearchAggregationService},
 * using mocked {@link SearchProvider}s (no Spring context / network).
 */
class SearchAggregationServiceTest {

    private final SearchProvider rezlive = mock(SearchProvider.class);
    private final SearchProvider hotelbeds = mock(SearchProvider.class);

    @BeforeEach
    void stubIds() {
        when(rezlive.id()).thenReturn(ProviderId.REZLIVE);
        when(hotelbeds.id()).thenReturn(ProviderId.HOTELBEDS);
    }

    private SearchAggregationService service() {
        return new SearchAggregationService(
                new SearchProviderRegistry(List.of(rezlive, hotelbeds), "rezlive"));
    }

    private static DestinationSearchRequest request() {
        return new DestinationSearchRequest(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3),
                "AE", "DXB", "AE", List.of(), List.of(new RoomRequest("R", 2, 0, List.of())));
    }

    private static SearchResult resultWith(String session, String currency, String... hotelNames) {
        List<SearchResult.FoundHotel> hotels = java.util.Arrays.stream(hotelNames)
                .map(n -> new SearchResult.FoundHotel(n + "-id", n, "4", "100", List.of()))
                .toList();
        return new SearchResult(session, currency, "AE", "2026-08-01", "2026-08-03", hotels, null);
    }

    @Test
    void fansOutToBothSuppliersAndTagsEachHotel() {
        when(rezlive.isConfigured()).thenReturn(true);
        when(hotelbeds.isConfigured()).thenReturn(true);
        when(rezlive.searchByDestination(any())).thenReturn(resultWith("sess-r", "USD", "Antalya Resort"));
        when(hotelbeds.searchByDestination(any())).thenReturn(resultWith("", "EUR", "Antalya Resort", "Beach Hotel"));

        CombinedSearchResult combined = service().searchByDestination(request(), null);

        assertThat(combined.hotels()).hasSize(3);
        assertThat(combined.hotels()).filteredOn(h -> h.provider().equals("REZLIVE"))
                .singleElement()
                .satisfies(h -> {
                    assertThat(h.searchSessionId()).isEqualTo("sess-r");
                    assertThat(h.currency()).isEqualTo("USD");
                });
        assertThat(combined.hotels()).filteredOn(h -> h.provider().equals("HOTELBEDS"))
                .hasSize(2)
                .allSatisfy(h -> assertThat(h.currency()).isEqualTo("EUR"));
        assertThat(combined.providerStatus())
                .extracting(CombinedSearchResult.ProviderStatus::provider,
                        CombinedSearchResult.ProviderStatus::ok, CombinedSearchResult.ProviderStatus::hotelCount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("REZLIVE", true, 1),
                        org.assertj.core.groups.Tuple.tuple("HOTELBEDS", true, 2));
    }

    @Test
    void oneSupplierFailingDoesNotBlankTheOther() {
        when(rezlive.isConfigured()).thenReturn(true);
        when(hotelbeds.isConfigured()).thenReturn(true);
        when(rezlive.searchByDestination(any())).thenReturn(resultWith("sess-r", "USD", "Antalya Resort"));
        when(hotelbeds.searchByDestination(any())).thenThrow(new UpstreamApiException("Hotelbeds 400: bad request"));

        CombinedSearchResult combined = service().searchByDestination(request(), null);

        assertThat(combined.hotels()).singleElement().satisfies(h -> assertThat(h.provider()).isEqualTo("REZLIVE"));
        assertThat(combined.providerStatus()).filteredOn(s -> s.provider().equals("HOTELBEDS"))
                .singleElement()
                .satisfies(s -> {
                    assertThat(s.ok()).isFalse();
                    assertThat(s.error()).contains("Hotelbeds 400");
                });
    }

    @Test
    void unconfiguredSupplierIsSkippedButReported() {
        when(rezlive.isConfigured()).thenReturn(true);
        when(hotelbeds.isConfigured()).thenReturn(false);
        when(rezlive.searchByDestination(any())).thenReturn(resultWith("sess-r", "USD", "Antalya Resort"));

        CombinedSearchResult combined = service().searchByDestination(request(), null);

        assertThat(combined.hotels()).singleElement().satisfies(h -> assertThat(h.provider()).isEqualTo("REZLIVE"));
        assertThat(combined.providerStatus()).filteredOn(s -> s.provider().equals("HOTELBEDS"))
                .singleElement()
                .satisfies(s -> {
                    assertThat(s.ok()).isFalse();
                    assertThat(s.error()).isEqualTo("not configured");
                });
        verify(hotelbeds, never()).searchByDestination(any());
    }

    @Test
    void failsWith503WhenNoSupplierIsConfigured() {
        when(rezlive.isConfigured()).thenReturn(false);
        when(hotelbeds.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> service().searchByDestination(request(), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No hotel supplier is configured");
    }

    @Test
    void explicitProviderQueriesOnlyThatSupplier() {
        when(rezlive.isConfigured()).thenReturn(true);
        when(hotelbeds.isConfigured()).thenReturn(true);
        when(hotelbeds.searchByDestination(any())).thenReturn(resultWith("", "EUR", "Beach Hotel"));

        CombinedSearchResult combined = service().searchByDestination(request(), ProviderId.HOTELBEDS);

        assertThat(combined.hotels()).singleElement().satisfies(h -> assertThat(h.provider()).isEqualTo("HOTELBEDS"));
        assertThat(combined.providerStatus()).singleElement()
                .satisfies(s -> assertThat(s.provider()).isEqualTo("HOTELBEDS"));
        verify(rezlive, never()).searchByDestination(any());
    }
}
