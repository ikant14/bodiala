package com.miirphys.bodiala.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.miirphys.bodiala.booking.HotelBooking;
import com.miirphys.bodiala.booking.HotelBookingRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BookingProviderRegistry#forBooking(String)} — the routing that sends an
 * operation back to the supplier that created the booking, keyed on its persisted {@code provider}
 * column. Uses mocks, so no Spring context / DB.
 */
class BookingProviderRegistryTest {

    private final BookingProvider hotelbeds = mock(BookingProvider.class);
    private final HotelBookingRepository bookings = mock(HotelBookingRepository.class);

    @BeforeEach
    void stubIds() {
        when(hotelbeds.id()).thenReturn(ProviderId.HOTELBEDS);
    }

    private BookingProviderRegistry registry() {
        return new BookingProviderRegistry(List.of(hotelbeds), "hotelbeds", bookings);
    }

    private void storedProviderIs(String provider) {
        HotelBooking booking = new HotelBooking();
        booking.setBookingId("BID");
        booking.setProvider(provider);
        when(bookings.findByBookingId("BID")).thenReturn(Optional.of(booking));
    }

    @Test
    void routesToTheSupplierThatCreatedTheBooking() {
        storedProviderIs("HOTELBEDS");
        assertThat(registry().forBooking("BID").id()).isEqualTo(ProviderId.HOTELBEDS);
    }

    @Test
    void storedProviderMatchIsCaseInsensitive() {
        storedProviderIs("hotelbeds");
        assertThat(registry().forBooking("BID").id()).isEqualTo(ProviderId.HOTELBEDS);
    }

    @Test
    void legacyNullRowFallsBackToHotelbeds() {
        storedProviderIs(null);
        assertThat(registry().forBooking("BID").id()).isEqualTo(ProviderId.HOTELBEDS);
    }

    @Test
    void unrecognisedStoredProviderIsAServerError() {
        storedProviderIs("EXPEDIA");
        assertThatThrownBy(() -> registry().forBooking("BID"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EXPEDIA");
    }

    @Test
    void unknownBookingIdIsNotFound() {
        when(bookings.findByBookingId("NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> registry().forBooking("NOPE"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void newBookingsDefaultToHotelbedsProvider() {
        assertThat(new HotelBooking().getProvider()).isEqualTo("HOTELBEDS");
    }
}
