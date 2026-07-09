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
 * Unit tests for {@link BookingProviderRegistry#forBooking(String)} — the new routing that sends an
 * operation back to the supplier that created the booking, keyed on its persisted {@code provider}
 * column. Uses mocks, so no Spring context / DB.
 */
class BookingProviderRegistryTest {

    private final BookingProvider rezlive = mock(BookingProvider.class);
    private final BookingProvider hotelbeds = mock(BookingProvider.class);
    private final HotelBookingRepository bookings = mock(HotelBookingRepository.class);

    @BeforeEach
    void stubIds() {
        when(rezlive.id()).thenReturn(ProviderId.REZLIVE);
        when(hotelbeds.id()).thenReturn(ProviderId.HOTELBEDS);
    }

    private BookingProviderRegistry registry(String defaultProvider) {
        return new BookingProviderRegistry(List.of(rezlive, hotelbeds), defaultProvider, bookings);
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
        assertThat(registry("rezlive").forBooking("BID").id()).isEqualTo(ProviderId.HOTELBEDS);
    }

    @Test
    void storedProviderMatchIsCaseInsensitive() {
        storedProviderIs("hotelbeds");
        assertThat(registry("rezlive").forBooking("BID").id()).isEqualTo(ProviderId.HOTELBEDS);
    }

    @Test
    void legacyNullRowRoutesToRezLiveEvenWhenTheDefaultIsHotelbeds() {
        storedProviderIs(null);
        assertThat(registry("hotelbeds").forBooking("BID").id()).isEqualTo(ProviderId.REZLIVE);
    }

    @Test
    void blankRowRoutesToRezLive() {
        storedProviderIs("   ");
        assertThat(registry("hotelbeds").forBooking("BID").id()).isEqualTo(ProviderId.REZLIVE);
    }

    @Test
    void unrecognisedStoredProviderIsAServerError() {
        storedProviderIs("EXPEDIA");
        assertThatThrownBy(() -> registry("rezlive").forBooking("BID"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EXPEDIA");
    }

    @Test
    void unknownBookingIdIsNotFound() {
        when(bookings.findByBookingId("NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> registry("rezlive").forBooking("NOPE"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void newBookingsDefaultToRezLiveProvider() {
        // RezLive book() never calls setProvider — it relies on this field default, which forBooking
        // reads back. Guard it so dropping the default fails here rather than misrouting at runtime.
        assertThat(new HotelBooking().getProvider()).isEqualTo("REZLIVE");
    }
}
