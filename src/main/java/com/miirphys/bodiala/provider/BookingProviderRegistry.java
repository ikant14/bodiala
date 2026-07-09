package com.miirphys.bodiala.provider;

import com.miirphys.bodiala.booking.HotelBooking;
import com.miirphys.bodiala.booking.HotelBookingRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Registry of the loaded {@link BookingProvider}s.
 *
 * <p>Requests that open a new interaction — {@code prebook}, {@code book}, and the pre-booking
 * cancellation-policy lookup — select their supplier with the {@code ?provider=} query param
 * ({@link #resolve(ProviderId)}). Operations keyed on an <em>existing</em> booking ({@code cancel},
 * {@code details}, {@code confirmation}, the post-booking policy, and the stored-booking read) must
 * go back to the supplier that created it, so they resolve from that booking's persisted
 * {@code provider} column via {@link #forBooking(String)} — the caller never has to pass a provider.
 */
@Component
public class BookingProviderRegistry extends ProviderRegistry<BookingProvider> {

    private final HotelBookingRepository bookings;

    public BookingProviderRegistry(List<BookingProvider> providers,
                                   @Value("${hotel.provider:hotelbeds}") String defaultProvider,
                                   HotelBookingRepository bookings) {
        super(providers, BookingProvider::id, ProviderId.fromOrDefault(defaultProvider, ProviderId.HOTELBEDS));
        this.bookings = bookings;
    }

    /**
     * Resolve the provider that owns the stored booking {@code bookingId}, keyed on its persisted
     * {@code provider} column. An unknown bookingId surfaces as 404 (NoSuchElement).
     */
    public BookingProvider forBooking(String bookingId) {
        HotelBooking booking = bookings.findByBookingId(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Unknown bookingId " + bookingId));
        return resolve(providerOf(booking));
    }

    /**
     * The supplier that owns a stored booking, from its persisted {@code provider} column. Null/blank
     * rows fall back to {@link ProviderId#HOTELBEDS} (the sole supplier), mirroring the
     * {@code HotelBooking.provider} field default. An unrecognised value is a server-side data
     * problem, surfaced as 503 (IllegalState) rather than a client-facing 400.
     */
    private static ProviderId providerOf(HotelBooking booking) {
        String provider = booking.getProvider();
        if (provider == null || provider.isBlank()) {
            return ProviderId.HOTELBEDS;
        }
        try {
            return ProviderId.from(provider);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Booking " + booking.getBookingId() + " has an unrecognised provider '" + provider + "'", e);
        }
    }
}
