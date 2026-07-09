package com.miirphys.bodiala.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Unit tests for the {@code ?provider=} → {@link ProviderId} binding. */
class ProviderIdConverterTest {

    private final ProviderIdConverter converter = new ProviderIdConverter();

    @Test
    void bindsCaseInsensitively() {
        assertThat(converter.convert("hotelbeds")).isEqualTo(ProviderId.HOTELBEDS);
        assertThat(converter.convert("HotelBeds")).isEqualTo(ProviderId.HOTELBEDS);
        assertThat(converter.convert("HOTELBEDS")).isEqualTo(ProviderId.HOTELBEDS);
        assertThat(converter.convert(" hotelbeds ")).isEqualTo(ProviderId.HOTELBEDS);
    }

    @Test
    void blankBindsToNullSoTheDefaultApplies() {
        assertThat(converter.convert("")).isNull();
        assertThat(converter.convert("   ")).isNull();
    }

    @Test
    void unknownProviderIsRejected() {
        assertThatThrownBy(() -> converter.convert("expedia"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
