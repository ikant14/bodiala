package com.miirphys.bodiala.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
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
import com.miirphys.bodiala.staticdata.domain.HotelImage;
import com.miirphys.bodiala.staticdata.repo.HotelImageRepository;
import com.miirphys.bodiala.staticdata.repo.HotelRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for the flatten / tag / partial-failure behaviour of {@link SearchAggregationService},
 * using a mocked {@link SearchProvider} (no Spring context / network). (Cross-supplier fan-out is
 * exercised again once a second provider exists.)
 */
class SearchAggregationServiceTest {

    private final SearchProvider hotelbeds = mock(SearchProvider.class);
    private final HotelRepository hotelRepo = mock(HotelRepository.class);
    private final HotelImageRepository imageRepo = mock(HotelImageRepository.class);

    @BeforeEach
    void stubId() {
        when(hotelbeds.id()).thenReturn(ProviderId.HOTELBEDS);
    }

    private SearchAggregationService service() {
        return new SearchAggregationService(new SearchProviderRegistry(List.of(hotelbeds), "hotelbeds"),
                hotelRepo, imageRepo);
    }

    private static DestinationSearchRequest request() {
        return request(List.of());
    }

    private static DestinationSearchRequest request(List<Integer> hotelRatings) {
        return request(List.of("PMI"), hotelRatings);
    }

    private static DestinationSearchRequest request(List<String> cities, List<Integer> hotelRatings) {
        return new DestinationSearchRequest(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3),
                "ES", cities, "ES", hotelRatings, List.of(new RoomRequest("R", 2, 0, List.of())));
    }

    private static SearchResult resultWith(String session, String currency, String... hotelNames) {
        List<SearchResult.FoundHotel> hotels = java.util.Arrays.stream(hotelNames)
                .map(n -> new SearchResult.FoundHotel(n + "-id", n, "PMI", "4", "100", List.of()))
                .toList();
        return new SearchResult(session, currency, "ES", "2026-08-01", "2026-08-03", hotels, null);
    }

    /** Build a result whose hotels carry the given star ratings (name = "H{rating}", or "H-null"). */
    private static SearchResult resultWithRatings(String... ratings) {
        List<SearchResult.FoundHotel> hotels = java.util.Arrays.stream(ratings)
                .map(r -> new SearchResult.FoundHotel("H" + r + "-id", "H" + r, "PMI", r, "100", List.of()))
                .toList();
        return new SearchResult("", "EUR", "ES", "2026-08-01", "2026-08-03", hotels, null);
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
    void hotelRatingsFilterKeepsOnlyMatchingStarsAndCountsFiltered() {
        when(hotelbeds.isConfigured()).thenReturn(true);
        when(hotelbeds.searchByDestination(any())).thenReturn(resultWithRatings("3", "4", "5"));

        // Non-contiguous selection: 3 and 5, but not 4.
        CombinedSearchResult combined = service().searchByDestination(request(List.of(3, 5)), null);

        assertThat(combined.hotels()).extracting(CombinedSearchResult.Hotel::name)
                .containsExactlyInAnyOrder("H3", "H5");
        // The provider status count reflects what's actually shown (post-filter).
        assertThat(combined.providerStatus()).singleElement()
                .satisfies(s -> assertThat(s.hotelCount()).isEqualTo(2));
    }

    @Test
    void hotelWithNoRatingIsExcludedWhenFilterIsActive() {
        when(hotelbeds.isConfigured()).thenReturn(true);
        when(hotelbeds.searchByDestination(any())).thenReturn(resultWithRatings("4", "null"));
        // "null" isn't a digit, so that hotel has an unparseable rating.

        CombinedSearchResult combined = service().searchByDestination(request(List.of(4)), null);

        assertThat(combined.hotels()).extracting(CombinedSearchResult.Hotel::name).containsExactly("H4");
    }

    @Test
    void emptyRatingsMeansNoFilter() {
        when(hotelbeds.isConfigured()).thenReturn(true);
        when(hotelbeds.searchByDestination(any())).thenReturn(resultWithRatings("3", "4", "5"));

        CombinedSearchResult combined = service().searchByDestination(request(List.of()), null);

        assertThat(combined.hotels()).hasSize(3);
    }

    @Test
    void singleCityUsesTheDestinationRoute() {
        when(hotelbeds.isConfigured()).thenReturn(true);
        when(hotelbeds.searchByDestination(any())).thenReturn(resultWith("", "EUR", "Palma Grand"));

        CombinedSearchResult combined = service().searchByDestination(request(List.of("PMI"), List.of()), null);

        assertThat(combined.hotels()).hasSize(1);
        verify(hotelbeds).searchByDestination(any());
        verify(hotelbeds, never()).searchByHotelIds(any());
    }

    @Test
    void multipleCitiesResolveCachedCodesAndSearchByHotelIdsInOneCall() {
        when(hotelbeds.isConfigured()).thenReturn(true);
        when(hotelRepo.findHotelCodesByCityCodeIn(any())).thenReturn(List.of(1067L, 3424L));
        when(hotelbeds.searchByHotelIds(any())).thenReturn(resultWith("", "EUR", "Palma Grand", "Barcelona Rambla"));

        CombinedSearchResult combined =
                service().searchByDestination(request(List.of("PMI", "BCN"), List.of()), null);

        // One by-hotel-ids call carrying the resolved codes; the destination route is not used.
        ArgumentCaptor<HotelIdsSearchRequest> ids = ArgumentCaptor.forClass(HotelIdsSearchRequest.class);
        verify(hotelbeds).searchByHotelIds(ids.capture());
        verify(hotelbeds, never()).searchByDestination(any());
        assertThat(ids.getValue().hotelIds()).containsExactly(1067L, 3424L);
        assertThat(combined.hotels()).hasSize(2);
    }

    @Test
    void multipleCitiesWithNoCachedHotelsReturnEmptyWithoutCallingTheSupplier() {
        when(hotelbeds.isConfigured()).thenReturn(true);
        when(hotelRepo.findHotelCodesByCityCodeIn(any())).thenReturn(List.of());

        CombinedSearchResult combined =
                service().searchByDestination(request(List.of("PMI", "BCN"), List.of()), null);

        assertThat(combined.hotels()).isEmpty();
        verify(hotelbeds, never()).searchByHotelIds(any());
        // Empty cache is not a supplier failure — the provider still reports ok with a zero count.
        assertThat(combined.providerStatus()).singleElement()
                .satisfies(s -> {
                    assertThat(s.ok()).isTrue();
                    assertThat(s.hotelCount()).isZero();
                });
    }

    @Test
    void emptyCitiesListIsRejected() {
        when(hotelbeds.isConfigured()).thenReturn(true);

        assertThatThrownBy(() -> service().searchByDestination(request(List.of(), List.of()), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one city");
    }

    @Test
    @SuppressWarnings("unchecked")
    void attachesCachedCoverImageAndLeavesUncachedHotelsWithout() {
        when(hotelbeds.isConfigured()).thenReturn(true);
        SearchResult result = new SearchResult("", "EUR", "ES", "2026-08-01", "2026-08-03", List.of(
                new SearchResult.FoundHotel("1067", "Palma Grand", "PMI", "4", "100", List.of()),
                new SearchResult.FoundHotel("9999", "Not Cached", "PMI", "3", "80", List.of())), null);
        when(hotelbeds.searchByDestination(any())).thenReturn(result);
        when(imageRepo.findCoverImages(any())).thenReturn(List.of(new HotelImage(1067L, "https://img/1067.jpg")));

        CombinedSearchResult combined = service().searchByDestination(request(), null);

        assertThat(combined.hotels()).extracting(CombinedSearchResult.Hotel::id, CombinedSearchResult.Hotel::image)
                .containsExactly(tuple("1067", "https://img/1067.jpg"), tuple("9999", null));
        // One lookup for the whole result, keyed by the numeric hotel codes.
        ArgumentCaptor<Collection<Long>> codes = ArgumentCaptor.forClass(Collection.class);
        verify(imageRepo).findCoverImages(codes.capture());
        assertThat(codes.getValue()).containsExactlyInAnyOrder(1067L, 9999L);
    }

    @Test
    void skipsTheImageLookupWhenNoHotelIdIsACacheCode() {
        when(hotelbeds.isConfigured()).thenReturn(true);
        when(hotelbeds.searchByDestination(any())).thenReturn(resultWith("", "EUR", "Palma Grand"));

        CombinedSearchResult combined = service().searchByDestination(request(), null);

        assertThat(combined.hotels()).singleElement().satisfies(h -> assertThat(h.image()).isNull());
        verify(imageRepo, never()).findCoverImages(any());
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
