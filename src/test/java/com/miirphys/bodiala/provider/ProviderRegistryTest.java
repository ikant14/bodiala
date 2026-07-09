package com.miirphys.bodiala.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for the generic {@link ProviderRegistry} resolution rules (no Spring context). */
class ProviderRegistryTest {

    /** A minimal thing keyed by {@link ProviderId} to exercise the registry. */
    private record Impl(ProviderId id) {
    }

    /** Concrete registry over {@link Impl} for testing. */
    private static final class ImplRegistry extends ProviderRegistry<Impl> {
        ImplRegistry(List<Impl> impls, ProviderId defaultId) {
            super(impls, Impl::id, defaultId);
        }
    }

    @Test
    void nullResolvesToDefault() {
        ImplRegistry registry = new ImplRegistry(List.of(new Impl(ProviderId.HOTELBEDS)), ProviderId.HOTELBEDS);

        assertThat(registry.resolve(null).id()).isEqualTo(ProviderId.HOTELBEDS);
        assertThat(registry.defaultId()).isEqualTo(ProviderId.HOTELBEDS);
    }

    @Test
    void explicitProviderIsHonoured() {
        ImplRegistry registry = new ImplRegistry(List.of(new Impl(ProviderId.HOTELBEDS)), ProviderId.HOTELBEDS);

        assertThat(registry.resolve(ProviderId.HOTELBEDS).id()).isEqualTo(ProviderId.HOTELBEDS);
    }

    @Test
    void unregisteredProviderIsRejected() {
        ImplRegistry registry = new ImplRegistry(List.of(), ProviderId.HOTELBEDS);

        assertThatThrownBy(() -> registry.resolve(ProviderId.HOTELBEDS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HOTELBEDS");
    }

    @Test
    void duplicateImplementationsAreRejectedAtStartup() {
        assertThatThrownBy(() -> new ImplRegistry(
                List.of(new Impl(ProviderId.HOTELBEDS), new Impl(ProviderId.HOTELBEDS)), ProviderId.HOTELBEDS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HOTELBEDS");
    }
}
