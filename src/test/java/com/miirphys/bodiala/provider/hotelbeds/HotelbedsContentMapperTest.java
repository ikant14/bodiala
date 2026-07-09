package com.miirphys.bodiala.provider.hotelbeds;

import static org.assertj.core.api.Assertions.assertThat;

import com.miirphys.bodiala.provider.hotelbeds.dto.HotelContentResponse;
import com.miirphys.bodiala.staticdata.domain.City;
import com.miirphys.bodiala.staticdata.domain.Country;
import com.miirphys.bodiala.staticdata.domain.Hotel;
import com.miirphys.bodiala.staticdata.domain.HotelImage;
import com.miirphys.bodiala.staticdata.domain.PropertyAmenity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Fixture-tests the Hotelbeds content → shared cache entity mapping (HTTP-free). */
class HotelbedsContentMapperTest {

    private static HotelContentResponse.Hotel sampleHotel() {
        return new HotelContentResponse.Hotel(
                112,
                new HotelContentResponse.Content("Aqualuz Suite Hotel", "ENG"),
                new HotelContentResponse.Content("Set on the seafront...", "ENG"),
                "PT",
                "FAO",
                new HotelContentResponse.Content("Lagos", "ENG"),
                "4EST",
                new HotelContentResponse.Content("Rua da Meia Praia, Lote 1", "ENG"),
                "8600-315",
                new HotelContentResponse.Coordinates(-8.66837, 37.10230),
                List.of(new HotelContentResponse.Image("00/000112/000112a_hb.jpg", "GEN", 1)),
                List.of(
                        facility(260, 70, true),                          // Bar — amenity, kept
                        facility(261, 70, true),                          // Wifi — amenity, kept
                        facility(1, 30, true),                            // Visa — payment group 30, filtered
                        number(30, 10, new BigDecimal("2018")),           // renovation year, filtered
                        distance(9, 10, new BigDecimal("150"))));         // POI distance, filtered
    }

    /** Names for the amenity codes above; anything else (should never be asked for) → null. */
    private static final FacilityNameResolver RESOLVER = (group, code) ->
            Map.of("70#260", "Bar", "70#261", "Wifi", "30#1", "Visa").get(group + "#" + code);

    private static HotelContentResponse.Facility facility(int code, int group, boolean present) {
        return new HotelContentResponse.Facility(code, group, present, null, null, null, null, null, null, null);
    }

    private static HotelContentResponse.Facility number(int code, int group, BigDecimal number) {
        return new HotelContentResponse.Facility(code, group, null, number, null, null, null, null, null, null);
    }

    private static HotelContentResponse.Facility distance(int code, int group, BigDecimal distance) {
        return new HotelContentResponse.Facility(code, group, null, null, distance, null, null, null, null, null);
    }

    @Test
    void mapsHotelWithCategoryCoordinatesAndDestinationAsCityCode() {
        Hotel hotel = HotelbedsContentMapper.toHotel(sampleHotel());
        assertThat(hotel.getHotelCode()).isEqualTo(112L);
        assertThat(hotel.getName()).isEqualTo("Aqualuz Suite Hotel");
        assertThat(hotel.getCityCode()).isEqualTo("FAO");        // destination code — what HB search sends
        assertThat(hotel.getCountryCode()).isEqualTo("PT");
        assertThat(hotel.getRating()).isEqualTo(4);              // "4EST" → 4
        assertThat(hotel.getLatitude()).isEqualTo(37.10230);
        assertThat(hotel.getLongitude()).isEqualTo(-8.66837);
        assertThat(hotel.getHotelPostalCode()).isEqualTo("8600-315");
    }

    @Test
    void mapsImagesFacilitiesAndDerivesCountryCity() {
        HotelContentResponse.Hotel h = sampleHotel();

        List<HotelImage> images = HotelbedsContentMapper.toImages(h);
        assertThat(images).hasSize(1);
        assertThat(images.get(0).getImage())
                .isEqualTo("https://photos.hotelbeds.com/giata/bigger/00/000112/000112a_hb.jpg");

        // Only the two real amenities survive: payment methods (grp 30), numeric metadata, and
        // distances are all filtered out; names come from the resolver (the /types/facilities master).
        PropertyAmenity amenity = HotelbedsContentMapper.toPropertyAmenity(h, RESOLVER);
        assertThat(amenity.getHotelAmenities()).isEqualTo("Bar,Wifi");

        Country country = HotelbedsContentMapper.toCountry(h);
        assertThat(country.getCountryCode()).isEqualTo("PT");

        City city = HotelbedsContentMapper.toCity(h);
        assertThat(city.getCityCode()).isEqualTo("FAO");
        assertThat(city.getName()).isEqualTo("Lagos");           // city.content
        assertThat(city.getCountryCode()).isEqualTo("PT");
    }
}
