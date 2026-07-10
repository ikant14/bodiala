package com.miirphys.bodiala.provider.hotelbeds;

import static org.assertj.core.api.Assertions.assertThat;

import com.miirphys.bodiala.provider.hotelbeds.dto.AvailabilityResponse;
import com.miirphys.bodiala.provider.model.SearchResult;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Fixture-tests the Hotelbeds availability → neutral SearchResult mapping (HTTP-free). */
class HotelbedsSearchMapperTest {

    @Test
    void mapsAvailabilityIntoNeutralSearchResult() {
        AvailabilityResponse.Rate recheck = new AvailabilityResponse.Rate(
                "RK-RECHECK", "NOR", "RECHECK", "354.39", null, "BB", "BED AND BREAKFAST", 1, 2, 1, "7");
        AvailabilityResponse.Rate bookable = new AvailabilityResponse.Rate(
                "RK-BOOKABLE", "NRF", "BOOKABLE", "398.21", "487.02", "BB", "BED AND BREAKFAST", 1, 2, 1, "7");
        AvailabilityResponse.Room room =
                new AvailabilityResponse.Room("DBL.ST", "DOUBLE STANDARD", List.of(recheck, bookable));
        AvailabilityResponse.Hotel hotel = new AvailabilityResponse.Hotel(
                3424, "As Americas", "4EST", "4 STARS", "CEN", "EUR", "354.39", "487.02", List.of(room));
        AvailabilityResponse response = new AvailabilityResponse(
                new AvailabilityResponse.Hotels("2026-09-15", "2026-09-18", 1, List.of(hotel)));

        SearchResult result = HotelbedsSearchMapper.toSearchResult(
                response, "GB", LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 18));

        assertThat(result.searchSessionId()).isEmpty();            // stateless
        assertThat(result.currency()).isEqualTo("EUR");
        assertThat(result.guestNationality()).isEqualTo("GB");
        assertThat(result.arrivalDate()).isEqualTo("2026-09-15");  // ISO
        assertThat(result.hotels()).hasSize(1);

        SearchResult.FoundHotel found = result.hotels().get(0);
        assertThat(found.id()).isEqualTo("3424");
        assertThat(found.name()).isEqualTo("As Americas");
        assertThat(found.city()).isEqualTo("CEN");                 // destinationCode → per-hotel city tag
        assertThat(found.rating()).isEqualTo("4");                 // "4EST" normalised to the numeric star
        assertThat(found.price()).isEqualTo("354.39");             // minRate
        assertThat(found.roomDetails()).hasSize(2);

        SearchResult.RoomDetail r0 = found.roomDetails().get(0);
        assertThat(r0.bookingKey()).isEqualTo("RK-RECHECK#RECHECK"); // rateType carried on the key
        assertThat(r0.totalRate()).isEqualTo("354.39");             // no sellingRate → net
        assertThat(r0.type()).isEqualTo("DOUBLE STANDARD");
        assertThat(r0.boardBasis()).isEqualTo("BED AND BREAKFAST");

        SearchResult.RoomDetail r1 = found.roomDetails().get(1);
        assertThat(r1.bookingKey()).isEqualTo("RK-BOOKABLE#BOOKABLE");
        assertThat(r1.totalRate()).isEqualTo("487.02");             // sellingRate preferred
    }

    @Test
    void emptyResponseYieldsNoHotels() {
        SearchResult result = HotelbedsSearchMapper.toSearchResult(
                new AvailabilityResponse(null), "GB", LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 18));
        assertThat(result.hotels()).isEmpty();
        assertThat(result.searchSessionId()).isEmpty();
    }
}
