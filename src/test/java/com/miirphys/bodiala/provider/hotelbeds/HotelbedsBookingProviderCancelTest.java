package com.miirphys.bodiala.provider.hotelbeds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.miirphys.bodiala.booking.HotelBooking;
import com.miirphys.bodiala.booking.HotelBookingRepository;
import com.miirphys.bodiala.provider.error.BookingNotCancellableException;
import com.miirphys.bodiala.provider.error.UpstreamApiException;
import com.miirphys.bodiala.provider.hotelbeds.dto.BookingResponse;
import com.miirphys.bodiala.provider.model.CancellationResult;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** How a Hotelbeds cancellation answer becomes our result or error (mocked client + repository). */
class HotelbedsBookingProviderCancelTest {

    private static final String REF = "102-21540932";

    private final HotelbedsJsonClient client = mock(HotelbedsJsonClient.class);
    private final HotelBookingRepository bookings = mock(HotelBookingRepository.class);
    private HotelbedsBookingProvider provider;
    private HotelBooking stored;

    @BeforeEach
    void setUp() {
        HotelbedsProperties props = new HotelbedsProperties();
        props.setApiKey("k");
        props.setSecret("s");
        provider = new HotelbedsBookingProvider(client, props, bookings);

        stored = new HotelBooking();
        stored.setBookingId(REF);
        stored.setStatus("Confirmed");
        stored.setCurrency("EUR");
        when(bookings.findByBookingId(REF)).thenReturn(Optional.of(stored));
    }

    private void hotelbedsAnswers(UpstreamApiException e) {
        when(client.delete(anyString(), eq(BookingResponse.class))).thenThrow(e);
    }

    @Test
    void cancelledBookingIsStoredWithItsCharges() {
        BookingResponse.Booking b = new BookingResponse.Booking(REF, "c", "CANCELLED", "CX-1", "EUR",
                new BookingResponse.Hotel(1, "Condado", "131.19", new BigDecimal("12.50")));
        when(client.delete(anyString(), eq(BookingResponse.class))).thenReturn(new BookingResponse(b));

        CancellationResult result = provider.cancel(REF);

        assertThat(result.status()).isEqualTo("CANCELLED");
        assertThat(stored.getStatus()).isEqualTo("Cancelled");
        assertThat(stored.getCancellationCharges()).isEqualTo("12.50");
        verify(bookings).save(stored);
    }

    @Test
    void bookingHotelbedsCannotSeeIsNotCancellableAndStaysAsStored() {
        hotelbedsAnswers(new UpstreamApiException(404,
                "Hotelbeds 404: The booking does not exist or you do not have permissions to access it."));

        assertThatThrownBy(() -> provider.cancel(REF))
                .isInstanceOf(BookingNotCancellableException.class)
                .hasMessageContaining(REF)
                .hasMessageContaining("another key");
        assertThat(stored.getStatus()).isEqualTo("Confirmed");
        verify(bookings, never()).save(any());
    }

    @Test
    void refusalPointingToCustomerServiceSuggestsRetryingThenCrc() {
        hotelbedsAnswers(new UpstreamApiException(410,
                "Hotelbeds 410: It was not possible to cancel the booking. Please, call our CRC to cancel it."));

        assertThatThrownBy(() -> provider.cancel(REF))
                .isInstanceOf(BookingNotCancellableException.class)
                .hasMessageContaining("try again")
                .hasMessageContaining("CRC");
        verify(bookings, never()).save(any());
    }

    @Test
    void alreadyCancelledBookingIsAnsweredFromStorageWithoutCallingHotelbeds() {
        stored.setStatus("Cancelled");
        stored.setCancellationCharges("0");

        CancellationResult result = provider.cancel(REF);

        assertThat(result.status()).isEqualTo("CANCELLED");
        assertThat(result.cancellationCharges()).isEqualTo("0");
        verifyNoInteractions(client);
    }

    @Test
    void otherSupplierErrorsStayUpstreamErrors() {
        UpstreamApiException outage = new UpstreamApiException(500, "Hotelbeds 500: Internal error");
        hotelbedsAnswers(outage);

        assertThatThrownBy(() -> provider.cancel(REF)).isSameAs(outage);
    }
}
