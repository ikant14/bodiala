package com.miirphys.bodiala.provider;

import com.miirphys.bodiala.booking.BookRequest;
import com.miirphys.bodiala.booking.HotelBooking;
import com.miirphys.bodiala.booking.PrebookRequest;
import com.miirphys.bodiala.provider.model.CancellationResult;
import com.miirphys.bodiala.provider.model.RateCheckResult;
import java.util.List;

/**
 * The booking chain, abstracted over the supplier. All implementations are loaded and indexed by
 * {@link BookingProviderRegistry}; new requests pick one via the {@code ?provider=} query param
 * (defaulting to {@code hotel.provider}), while operations on an existing booking route to the
 * supplier that created it (resolved from the stored {@code provider} column).
 *
 * <p>The core, frontend-driven flow returns provider-neutral results: {@link #prebook} →
 * {@link RateCheckResult}, {@link #book} → the persisted {@link HotelBooking}, {@link #cancel} →
 * {@link CancellationResult}. {@link #book}/{@link #cancel} implementations MUST NOT be
 * {@code @Transactional} (the supplier HTTP call must not run inside a DB transaction). The stored
 * booking carries its {@code provider}, so {@link #listBookings()}/{@link #getStored(String)} read
 * the shared table regardless of who created the row.
 */
public interface BookingProvider {

    ProviderId id();

    boolean isConfigured();

    RateCheckResult prebook(PrebookRequest request);

    HotelBooking book(BookRequest request);

    CancellationResult cancel(String bookingId);

    List<HotelBooking> listBookings();

    HotelBooking getStored(String bookingId);
}
