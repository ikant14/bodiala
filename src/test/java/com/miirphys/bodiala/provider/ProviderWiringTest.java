package com.miirphys.bodiala.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies that BOTH suppliers are now loaded together and indexed by the registries, and that an
 * unspecified provider resolves to the default (rezlive, from {@code hotel.provider}).
 */
@SpringBootTest
class ProviderWiringTest {

    @Autowired
    SearchProviderRegistry searchProviders;

    @Autowired
    BookingProviderRegistry bookingProviders;

    @Test
    void bothProvidersAreLoaded() {
        assertThat(searchProviders.resolve(ProviderId.REZLIVE).id()).isEqualTo(ProviderId.REZLIVE);
        assertThat(searchProviders.resolve(ProviderId.HOTELBEDS).id()).isEqualTo(ProviderId.HOTELBEDS);
        assertThat(bookingProviders.resolve(ProviderId.REZLIVE).id()).isEqualTo(ProviderId.REZLIVE);
        assertThat(bookingProviders.resolve(ProviderId.HOTELBEDS).id()).isEqualTo(ProviderId.HOTELBEDS);
    }

    @Test
    void defaultsToRezLiveWhenUnspecified() {
        assertThat(searchProviders.defaultId()).isEqualTo(ProviderId.REZLIVE);
        assertThat(searchProviders.resolve(null).id()).isEqualTo(ProviderId.REZLIVE);
        assertThat(bookingProviders.resolve(null).id()).isEqualTo(ProviderId.REZLIVE);
    }
}
