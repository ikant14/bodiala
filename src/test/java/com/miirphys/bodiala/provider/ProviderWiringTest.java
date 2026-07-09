package com.miirphys.bodiala.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies the Hotelbeds provider is loaded and indexed by the registries, and that both the
 * default and an explicit {@code ?provider=hotelbeds} resolve to it.
 */
@SpringBootTest
class ProviderWiringTest {

    @Autowired
    SearchProviderRegistry searchProviders;

    @Autowired
    BookingProviderRegistry bookingProviders;

    @Test
    void hotelbedsIsLoadedAndDefault() {
        assertThat(searchProviders.defaultId()).isEqualTo(ProviderId.HOTELBEDS);
        assertThat(searchProviders.resolve(null).id()).isEqualTo(ProviderId.HOTELBEDS);
        assertThat(searchProviders.resolve(ProviderId.HOTELBEDS).id()).isEqualTo(ProviderId.HOTELBEDS);

        assertThat(bookingProviders.resolve(null).id()).isEqualTo(ProviderId.HOTELBEDS);
        assertThat(bookingProviders.resolve(ProviderId.HOTELBEDS).id()).isEqualTo(ProviderId.HOTELBEDS);
    }
}
