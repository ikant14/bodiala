package com.miirphys.bodiala.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link ProviderId} parsing. */
class ProviderIdTest {

    @Test
    void fromParsesKnownValuesCaseInsensitively() {
        assertThat(ProviderId.from("hotelbeds")).isEqualTo(ProviderId.HOTELBEDS);
        assertThat(ProviderId.from(" HOTELBEDS ")).isEqualTo(ProviderId.HOTELBEDS);
    }

    @Test
    void fromThrowsOnUnknownOrBlank() {
        assertThatThrownBy(() -> ProviderId.from("rezlive")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProviderId.from("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromOrDefaultFallsBackInsteadOfThrowing() {
        // A stale hotel.provider (e.g. the removed 'rezlive') must NOT brick startup.
        assertThat(ProviderId.fromOrDefault("rezlive", ProviderId.HOTELBEDS)).isEqualTo(ProviderId.HOTELBEDS);
        assertThat(ProviderId.fromOrDefault("", ProviderId.HOTELBEDS)).isEqualTo(ProviderId.HOTELBEDS);
        assertThat(ProviderId.fromOrDefault(null, ProviderId.HOTELBEDS)).isEqualTo(ProviderId.HOTELBEDS);
        assertThat(ProviderId.fromOrDefault("hotelbeds", ProviderId.HOTELBEDS)).isEqualTo(ProviderId.HOTELBEDS);
    }
}
