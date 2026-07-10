package com.miirphys.bodiala.provider.hotelbeds;

import com.miirphys.bodiala.provider.hotelbeds.dto.AvailabilityResponse;
import com.miirphys.bodiala.provider.model.SearchResult;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

/**
 * Maps a Hotelbeds availability response into the neutral {@link SearchResult}. Pure / HTTP-free so
 * it can be fixture-tested offline (no quota spent).
 */
public final class HotelbedsSearchMapper {

    private HotelbedsSearchMapper() {
    }

    public static SearchResult toSearchResult(AvailabilityResponse response, String guestNationality,
                                              LocalDate arrival, LocalDate departure) {
        List<AvailabilityResponse.Hotel> hotels =
                (response == null || response.hotels() == null || response.hotels().hotels() == null)
                        ? List.of() : response.hotels().hotels();
        String currency = hotels.isEmpty() ? null : hotels.get(0).currency();
        List<SearchResult.FoundHotel> found = hotels.stream().map(HotelbedsSearchMapper::toHotel).toList();
        return new SearchResult(
                "",                                            // Hotelbeds is stateless — no session id
                currency,
                guestNationality,
                arrival == null ? null : arrival.toString(),   // ISO — no dd/MM/yyyy conversion
                departure == null ? null : departure.toString(),
                found,
                null);
    }

    private static SearchResult.FoundHotel toHotel(AvailabilityResponse.Hotel h) {
        List<SearchResult.RoomDetail> rooms = h.rooms() == null ? List.of()
                : h.rooms().stream().flatMap(HotelbedsSearchMapper::toRooms).toList();
        // Normalise the star rating to the numeric value (e.g. "4EST" → "4") so it matches the cached
        // detail-view rating and the frontend can render / filter it as a number.
        Integer stars = HotelbedsCategory.stars(h.categoryCode());
        String rating = stars == null ? null : String.valueOf(stars);
        // destinationCode tags each hotel with its city so a multi-city result can label/route per hotel.
        return new SearchResult.FoundHotel(String.valueOf(h.code()), h.name(), h.destinationCode(), rating,
                h.minRate(), rooms);
    }

    private static Stream<SearchResult.RoomDetail> toRooms(AvailabilityResponse.Room room) {
        if (room.rates() == null) {
            return Stream.empty();
        }
        return room.rates().stream().map(rate -> toRoomDetail(room, rate));
    }

    private static SearchResult.RoomDetail toRoomDetail(AvailabilityResponse.Room room, AvailabilityResponse.Rate rate) {
        // Carry rateType on the bookingKey so the booking provider knows whether /checkrates is required.
        String bookingKey = rate.rateType() == null ? rate.rateKey() : rate.rateKey() + "#" + rate.rateType();
        // Show the customer-facing price (sellingRate) when present, else the net cost.
        String totalRate = (rate.sellingRate() != null && !rate.sellingRate().isBlank())
                ? rate.sellingRate() : rate.net();
        return new SearchResult.RoomDetail(
                room.name(), bookingKey, str(rate.adults()), str(rate.children()),
                rate.childrenAges(), str(rate.rooms()), totalRate, room.name(), rate.boardName());
    }

    private static String str(Integer i) {
        return i == null ? null : String.valueOf(i);
    }
}
