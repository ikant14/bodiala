package com.miirphys.bodiala.provider.hotelbeds;

import com.miirphys.bodiala.provider.ProviderId;
import com.miirphys.bodiala.provider.SearchProvider;
import com.miirphys.bodiala.provider.hotelbeds.dto.AvailabilityResponse;
import com.miirphys.bodiala.provider.model.SearchResult;
import com.miirphys.bodiala.search.DestinationSearchRequest;
import com.miirphys.bodiala.search.HotelIdsSearchRequest;
import com.miirphys.bodiala.search.RoomRequest;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Hotelbeds-backed {@link SearchProvider}: builds the APItude availability request, calls
 * {@code POST /hotel-api/1.0/hotels}, and maps the response into the neutral {@link SearchResult}.
 * Always loaded and registered under {@link ProviderId#HOTELBEDS}; a request selects it via
 * {@code ?provider=hotelbeds}.
 *
 * <p>Destination search sends the request {@code city} as the Hotelbeds destination code, and
 * by-hotel-ids sends the numeric hotel codes directly — both assume the static-data cache holds
 * Hotelbeds codes (synced in a later content phase). Dates stay ISO (no {@code dd/MM/yyyy}).
 */
@Component
public class HotelbedsSearchProvider implements SearchProvider {

    private static final String AVAILABILITY = "/hotel-api/1.0/hotels";

    private final HotelbedsJsonClient client;
    private final HotelbedsProperties properties;

    public HotelbedsSearchProvider(HotelbedsJsonClient client, HotelbedsProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public ProviderId id() {
        return ProviderId.HOTELBEDS;
    }

    @Override
    public boolean isConfigured() {
        return properties.hasCredentials();
    }

    @Override
    public SearchResult searchByDestination(DestinationSearchRequest request) {
        requireCredentials();
        validate(request.arrivalDate(), request.departureDate(), request.rooms());
        // Hotelbeds availability takes a single destination code; multi-city is routed through
        // searchByHotelIds (resolved from the cache) by SearchAggregationService before reaching here.
        List<String> cities = request.cities();
        if (cities == null || cities.stream().allMatch(HotelbedsSearchProvider::isBlank)) {
            throw new IllegalArgumentException("at least one city (Hotelbeds destination code) is required "
                    + "for destination search");
        }
        if (cities.size() != 1) {
            throw new IllegalArgumentException("Hotelbeds destination search takes exactly one city; got "
                    + cities.size() + ". Multiple cities are searched via cached hotel ids.");
        }
        String city = cities.get(0).trim();
        if (city.length() > 3) {
            throw new IllegalArgumentException("Hotelbeds destination code must be 1-3 characters (e.g. DXB, PMI); got '"
                    + city + "'. Use a Hotelbeds destination code — from a Hotelbeds catalog import "
                    + "(POST /api/static-data/import?provider=hotelbeds) — not a plain city name/number.");
        }
        Map<String, Object> body = baseRequest(request.arrivalDate(), request.departureDate(), request.rooms());
        body.put("destination", Map.of("code", city));
        return search(body, request.guestNationality(), request.arrivalDate(), request.departureDate());
    }

    @Override
    public SearchResult searchByHotelIds(HotelIdsSearchRequest request) {
        requireCredentials();
        validate(request.arrivalDate(), request.departureDate(), request.rooms());
        if (request.hotelIds() == null || request.hotelIds().isEmpty()) {
            throw new IllegalArgumentException("hotelIds is required for search-by-hotel-ids");
        }
        Map<String, Object> body = baseRequest(request.arrivalDate(), request.departureDate(), request.rooms());
        body.put("hotels", Map.of("hotel", request.hotelIds()));
        return search(body, request.guestNationality(), request.arrivalDate(), request.departureDate());
    }

    private SearchResult search(Map<String, Object> body, String guestNationality,
                                LocalDate arrival, LocalDate departure) {
        AvailabilityResponse response = client.post(AVAILABILITY, body, AvailabilityResponse.class);
        return HotelbedsSearchMapper.toSearchResult(response, guestNationality, arrival, departure);
    }

    private Map<String, Object> baseRequest(LocalDate arrival, LocalDate departure, List<RoomRequest> rooms) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("stay", Map.of("checkIn", arrival.toString(), "checkOut", departure.toString()));
        body.put("occupancies", rooms.stream().map(HotelbedsSearchProvider::occupancy).toList());
        body.put("language", properties.getLanguage());
        if (!isBlank(properties.getSourceMarket())) {
            body.put("sourceMarket", properties.getSourceMarket());
        }
        return body;
    }

    private static Map<String, Object> occupancy(RoomRequest room) {
        int adults = room.noOfAdults() == null ? 1 : room.noOfAdults();
        int children = room.noOfChilds() == null ? 0 : room.noOfChilds();
        Map<String, Object> occ = new LinkedHashMap<>();
        occ.put("rooms", 1);
        occ.put("adults", adults);
        occ.put("children", children);
        if (children > 0 && room.childrenAges() != null && !room.childrenAges().isEmpty()) {
            occ.put("paxes", room.childrenAges().stream()
                    .map(age -> Map.of("type", "CH", "age", age)).toList());
        }
        return occ;
    }

    private void validate(LocalDate arrival, LocalDate departure, List<RoomRequest> rooms) {
        if (arrival == null || departure == null) {
            throw new IllegalArgumentException("arrivalDate and departureDate are required");
        }
        if (!departure.isAfter(arrival)) {
            throw new IllegalArgumentException("departureDate must be after arrivalDate");
        }
        if (rooms == null || rooms.isEmpty()) {
            throw new IllegalArgumentException("at least one room is required");
        }
        for (int i = 0; i < rooms.size(); i++) {
            Integer adults = rooms.get(i).noOfAdults();
            if (adults != null && adults < 1) {
                throw new IllegalArgumentException("room " + (i + 1)
                        + " needs at least 1 adult (Hotelbeds requires adults >= 1); got " + adults);
            }
        }
    }

    private void requireCredentials() {
        if (!properties.hasCredentials()) {
            throw new IllegalStateException(
                    "Hotelbeds credentials are not configured (hotelbeds.api-key / hotelbeds.secret). "
                            + "Cannot search until they are set.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
