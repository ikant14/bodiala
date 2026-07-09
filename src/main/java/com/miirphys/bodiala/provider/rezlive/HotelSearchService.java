package com.miirphys.bodiala.provider.rezlive;

import com.miirphys.bodiala.search.DestinationSearchRequest;
import com.miirphys.bodiala.search.HotelIdsSearchRequest;
import com.miirphys.bodiala.search.RoomRequest;
import com.miirphys.bodiala.provider.rezlive.client.RezLiveApiException;
import com.miirphys.bodiala.provider.rezlive.client.RezLiveXmlClient;
import com.miirphys.bodiala.provider.rezlive.client.dto.common.Authentication;
import com.miirphys.bodiala.provider.rezlive.client.dto.search.HotelFindRequest;
import com.miirphys.bodiala.provider.rezlive.client.dto.search.HotelFindRequest.Booking;
import com.miirphys.bodiala.provider.rezlive.client.dto.search.HotelFindRequest.Room;
import com.miirphys.bodiala.provider.rezlive.client.dto.search.HotelFindResponse;
import com.miirphys.bodiala.provider.rezlive.RezLiveProperties;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Live hotel availability search via RezLive {@code findhotel} / {@code findhotelbyid}.
 * Adapts our clean request objects to the RezLive XML contract (including ISO→dd/MM/yyyy dates)
 * and reuses {@link RezLiveXmlClient} for transport.
 */
@Service
public class HotelSearchService {

    private static final DateTimeFormatter REZLIVE_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RezLiveXmlClient client;
    private final RezLiveProperties properties;

    public HotelSearchService(RezLiveXmlClient client, RezLiveProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public HotelFindResponse searchByDestination(DestinationSearchRequest request) {
        requireCredentials();
        validateCommon(request.arrivalDate(), request.departureDate(), request.rooms());
        if (isBlank(request.city())) {
            throw new IllegalArgumentException("city (numeric city code) is required for destination search");
        }
        Booking booking = baseBooking(request.arrivalDate(), request.departureDate(),
                request.countryCode(), request.city(), request.guestNationality(), request.rooms());
        booking.setHotelRatings(request.hotelRatings());
        return execute("findhotel", booking);
    }

    public HotelFindResponse searchByHotelIds(HotelIdsSearchRequest request) {
        requireCredentials();
        validateCommon(request.arrivalDate(), request.departureDate(), request.rooms());
        if (request.hotelIds() == null || request.hotelIds().isEmpty()) {
            throw new IllegalArgumentException("hotelIds is required for search-by-hotel-ids");
        }
        if (isBlank(request.city())) {
            throw new IllegalArgumentException("city (numeric city code) is required for search-by-hotel-ids");
        }
        Booking booking = baseBooking(request.arrivalDate(), request.departureDate(),
                request.countryCode(), request.city(), request.guestNationality(), request.rooms());
        booking.setHotelIds(request.hotelIds());
        return execute("findhotelbyid", booking);
    }

    private HotelFindResponse execute(String action, Booking booking) {
        HotelFindRequest request = new HotelFindRequest(
                new Authentication(properties.getAgentCode(), properties.getUserName()), booking);
        HotelFindResponse response = client.execute(action, request, HotelFindResponse.class);
        if (response.hasError()) {
            throw new RezLiveApiException(response.getError());
        }
        return response;
    }

    private Booking baseBooking(LocalDate arrival, LocalDate departure, String countryCode,
                                String city, String guestNationality, List<RoomRequest> rooms) {
        Booking booking = new Booking();
        booking.setArrivalDate(arrival.format(REZLIVE_DATE));
        booking.setDepartureDate(departure.format(REZLIVE_DATE));
        booking.setCountryCode(countryCode);
        booking.setCity(city);
        booking.setGuestNationality(guestNationality);
        booking.setRooms(rooms.stream().map(HotelSearchService::toRoom).toList());
        return booking;
    }

    private static Room toRoom(RoomRequest source) {
        Room room = new Room();
        room.setType(source.type() != null ? source.type() : "Room-1");
        room.setNoOfAdults(source.noOfAdults() != null ? source.noOfAdults() : 1);
        int children = source.noOfChilds() != null ? source.noOfChilds() : 0;
        room.setNoOfChilds(children);
        if (children > 0) {
            room.setChildrenAges(source.childrenAges());
        }
        return room;
    }

    private void validateCommon(LocalDate arrival, LocalDate departure, List<RoomRequest> rooms) {
        if (arrival == null || departure == null) {
            throw new IllegalArgumentException("arrivalDate and departureDate are required");
        }
        if (!departure.isAfter(arrival)) {
            throw new IllegalArgumentException("departureDate must be after arrivalDate");
        }
        if (rooms == null || rooms.isEmpty()) {
            throw new IllegalArgumentException("at least one room is required");
        }
        for (RoomRequest room : rooms) {
            int children = room.noOfChilds() != null ? room.noOfChilds() : 0;
            int ages = room.childrenAges() == null ? 0 : room.childrenAges().size();
            if (children > 0 && ages != children) {
                throw new IllegalArgumentException(
                        "childrenAges must list one age per child (expected " + children + ", got " + ages + ")");
            }
        }
    }

    private void requireCredentials() {
        if (!properties.hasCredentials()) {
            throw new IllegalStateException(
                    "RezLive credentials are not configured (rezlive.agent-code / rezlive.user-name / "
                            + "rezlive.api-key). Cannot search until credentials and IP whitelisting are in place.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
