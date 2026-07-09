package com.miirphys.bodiala.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies that {@code hotel.provider=hotelbeds} only changes the DEFAULT supplier: an unspecified
 * provider now resolves to Hotelbeds, while RezLive is still loaded and reachable via
 * {@code ?provider=rezlive} (both suppliers load regardless of the switch).
 */
@SpringBootTest(properties = "hotel.provider=hotelbeds")
class HotelbedsProviderWiringTest {

    @Autowired
    SearchProviderRegistry searchProviders;

    @Autowired
    BookingProviderRegistry bookingProviders;

    @Test
    void hotelbedsIsTheDefaultButRezLiveStillLoads() {
        assertThat(searchProviders.defaultId()).isEqualTo(ProviderId.HOTELBEDS);
        assertThat(searchProviders.resolve(null).id()).isEqualTo(ProviderId.HOTELBEDS);
        assertThat(searchProviders.resolve(ProviderId.REZLIVE).id()).isEqualTo(ProviderId.REZLIVE);

        assertThat(bookingProviders.resolve(null).id()).isEqualTo(ProviderId.HOTELBEDS);
        assertThat(bookingProviders.resolve(ProviderId.REZLIVE).id()).isEqualTo(ProviderId.REZLIVE);
    }
}
