package com.miirphys.bodiala.provider.hotelbeds;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * The four "secondary" booking reads are not yet mapped for Hotelbeds and must throw
 * {@link UnsupportedOperationException} (→ HTTP 501 via the global handler). Each just throws before
 * touching any collaborator, so the provider can be built with nulls.
 */
class HotelbedsBookingProviderSecondaryReadsTest {

    private final HotelbedsBookingProvider provider = new HotelbedsBookingProvider(null, null, null);

    @Test
    void getBookingDetailsIsUnsupported() {
        assertThatThrownBy(() -> provider.getBookingDetails("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void confirmationDetailsIsUnsupported() {
        assertThatThrownBy(() -> provider.confirmationDetails("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void cancellationPolicyAfterBookingIsUnsupported() {
        assertThatThrownBy(() -> provider.cancellationPolicyAfterBooking("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void preBookingCancellationPolicyIsUnsupported() {
        assertThatThrownBy(() -> provider.cancellationPolicy(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
