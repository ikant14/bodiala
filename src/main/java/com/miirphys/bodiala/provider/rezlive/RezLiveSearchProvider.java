package com.miirphys.bodiala.provider.rezlive;

import com.miirphys.bodiala.provider.ProviderId;
import com.miirphys.bodiala.provider.SearchProvider;
import com.miirphys.bodiala.provider.model.SearchResult;
import com.miirphys.bodiala.provider.rezlive.client.dto.search.HotelFindResponse;
import com.miirphys.bodiala.search.DestinationSearchRequest;
import com.miirphys.bodiala.search.HotelIdsSearchRequest;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * RezLive-backed {@link SearchProvider}: delegates to {@link HotelSearchService} and maps the
 * RezLive {@code HotelFindResponse} into the neutral {@link SearchResult}. Always loaded and
 * registered under {@link ProviderId#REZLIVE}; a request selects it via {@code ?provider=rezlive}
 * (the default when none is given).
 */
@Component
public class RezLiveSearchProvider implements SearchProvider {

    private final HotelSearchService service;
    private final RezLiveProperties properties;

    public RezLiveSearchProvider(HotelSearchService service, RezLiveProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @Override
    public ProviderId id() {
        return ProviderId.REZLIVE;
    }

    @Override
    public boolean isConfigured() {
        return properties.hasCredentials();
    }

    @Override
    public SearchResult searchByDestination(DestinationSearchRequest request) {
        return toSearchResult(service.searchByDestination(request));
    }

    @Override
    public SearchResult searchByHotelIds(HotelIdsSearchRequest request) {
        return toSearchResult(service.searchByHotelIds(request));
    }

    private static SearchResult toSearchResult(HotelFindResponse r) {
        List<SearchResult.FoundHotel> hotels = r.getHotels() == null ? List.of()
                : r.getHotels().stream().map(RezLiveSearchProvider::toHotel).toList();
        return new SearchResult(r.getSearchSessionId(), r.getCurrency(), r.getGuestNationality(),
                r.getArrivalDate(), r.getDepartureDate(), hotels, r.getError());
    }

    private static SearchResult.FoundHotel toHotel(HotelFindResponse.FoundHotel h) {
        List<SearchResult.RoomDetail> rooms = h.getRoomDetails() == null ? List.of()
                : h.getRoomDetails().stream().map(RezLiveSearchProvider::toRoom).toList();
        return new SearchResult.FoundHotel(h.getId(), h.getName(), h.getRating(), h.getPrice(), rooms);
    }

    private static SearchResult.RoomDetail toRoom(HotelFindResponse.RoomDetail rd) {
        return new SearchResult.RoomDetail(rd.getType(), rd.getBookingKey(), rd.getAdults(),
                rd.getChildren(), rd.getChildrenAges(), rd.getTotalRooms(), rd.getTotalRate(),
                rd.getRoomDescription(), rd.getBoardBasis());
    }
}
