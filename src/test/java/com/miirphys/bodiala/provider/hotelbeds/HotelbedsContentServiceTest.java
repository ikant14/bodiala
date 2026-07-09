package com.miirphys.bodiala.provider.hotelbeds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miirphys.bodiala.provider.hotelbeds.dto.HotelContentResponse;
import com.miirphys.bodiala.staticdata.domain.Hotel;
import com.miirphys.bodiala.staticdata.repo.HotelImageRepository;
import com.miirphys.bodiala.staticdata.repo.HotelRepository;
import com.miirphys.bodiala.staticdata.repo.PropertyAmenityRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/** Unit tests for the on-demand single-hotel content fetch + lazy cache (mocked client + repos). */
class HotelbedsContentServiceTest {

    private final HotelbedsJsonClient client = mock(HotelbedsJsonClient.class);
    private final HotelbedsFacilityCatalog facilityCatalog = mock(HotelbedsFacilityCatalog.class);
    private final HotelRepository hotels = mock(HotelRepository.class);
    private final HotelImageRepository images = mock(HotelImageRepository.class);
    private final PropertyAmenityRepository amenities = mock(PropertyAmenityRepository.class);

    private HotelbedsContentService service(HotelbedsProperties props) {
        return new HotelbedsContentService(client, props, facilityCatalog, hotels, images, amenities);
    }

    private static HotelbedsProperties configured() {
        HotelbedsProperties p = new HotelbedsProperties();
        p.setApiKey("k");
        p.setSecret("s");
        return p; // baseUrl defaults, so hasCredentials() is true
    }

    private static HotelContentResponse.Content content(String value) {
        return new HotelContentResponse.Content(value, "ENG");
    }

    private static HotelContentResponse oneHotel(int code) {
        HotelContentResponse.Hotel h = new HotelContentResponse.Hotel(
                code, content("Seramar Comodoro Playa"), content("A hotel"), "ES", "PMI",
                content("Palma"), "4EST", content("1 Beach Rd"), "07000",
                new HotelContentResponse.Coordinates(2.6, 39.5),
                List.of(new HotelContentResponse.Image("00/000001/000001a_hb.jpg", "GEN", 1)),
                List.of(new HotelContentResponse.Facility(260, 70, true, null, null, null, null, null, null, null)));
        return new HotelContentResponse(List.of(h), 1, 1, 1);
    }

    @Test
    void fetchesByCodeThenMapsAndCaches() {
        when(client.get(anyString(), eq(HotelContentResponse.class))).thenReturn(oneHotel(12345));
        when(hotels.save(any(Hotel.class))).thenAnswer(i -> i.getArgument(0));
        when(facilityCatalog.nameFor(any(), any())).thenReturn("Bar");

        Hotel result = service(configured()).fetchAndCache(12345L);

        assertThat(result.getName()).isEqualTo("Seramar Comodoro Playa");
        assertThat(result.getCityCode()).isEqualTo("PMI");     // Hotelbeds destinationCode
        assertThat(result.getRating()).isEqualTo(4);           // from "4EST"
        verify(client).get(contains("codes=12345"), eq(HotelContentResponse.class));
        verify(hotels).save(any(Hotel.class));
        verify(images).deleteByHotelCode(12345L);              // replace this hotel's images
        verify(images).saveAll(any());
        verify(amenities).save(any());                         // "Bar" facility
    }

    @Test
    void unknownCodeIsNotFound() {
        when(client.get(anyString(), eq(HotelContentResponse.class)))
                .thenReturn(new HotelContentResponse(List.of(), 1, 1, 0));

        assertThatThrownBy(() -> service(configured()).fetchAndCache(999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("999");
    }

    @Test
    void missingCredentialsIsServiceError() {
        assertThatThrownBy(() -> service(new HotelbedsProperties()).fetchAndCache(1L))
                .isInstanceOf(IllegalStateException.class);
    }
}
