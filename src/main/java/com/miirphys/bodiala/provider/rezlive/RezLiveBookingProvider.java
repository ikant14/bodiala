package com.miirphys.bodiala.provider.rezlive;

import com.miirphys.bodiala.booking.BookRequest;
import com.miirphys.bodiala.booking.CancellationPolicyLookupRequest;
import com.miirphys.bodiala.booking.HotelBooking;
import com.miirphys.bodiala.booking.PrebookRequest;
import com.miirphys.bodiala.provider.BookingProvider;
import com.miirphys.bodiala.provider.ProviderId;
import com.miirphys.bodiala.provider.model.CancellationResult;
import com.miirphys.bodiala.provider.model.RateCheckResult;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.book.GetBookingResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.book.PreBookingResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation.CancellationInformations;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation.CancellationPolicyAfterBookingResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation.CancellationPolicyResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation.CancellationResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.confirmation.HotelConfirmationResponse;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * RezLive-backed {@link BookingProvider}: delegates to {@link BookingService} and maps the
 * neutralised results ({@code prebook} → {@link RateCheckResult}, {@code cancel} →
 * {@link CancellationResult}). Always loaded and registered under {@link ProviderId#REZLIVE}; new
 * requests select it via {@code ?provider=rezlive} (the default), while operations on an existing
 * booking route here automatically when the stored row's {@code provider} is REZLIVE. The
 * non-{@code @Transactional} nature of {@code book}/{@code cancel} is preserved by the service.
 */
@Component
public class RezLiveBookingProvider implements BookingProvider {

    private final BookingService service;
    private final RezLiveProperties properties;

    public RezLiveBookingProvider(BookingService service, RezLiveProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @Override
    public ProviderId id() {
        return ProviderId.REZLIVE;
    }

    @Override
    public boolean isConfigured() {
        return properties.hasCredentials();
    }

    @Override
    public RateCheckResult prebook(PrebookRequest request) {
        return toRateCheckResult(service.prebook(request));
    }

    @Override
    public HotelBooking book(BookRequest request) {
        return service.book(request);
    }

    @Override
    public CancellationResult cancel(String bookingId) {
        return toCancellationResult(service.cancel(bookingId));
    }

    @Override
    public List<HotelBooking> listBookings() {
        return service.listBookings();
    }

    @Override
    public HotelBooking getStored(String bookingId) {
        return service.getStored(bookingId);
    }

    @Override
    public GetBookingResponse getBookingDetails(String bookingId) {
        return service.getBookingDetails(bookingId);
    }

    @Override
    public HotelConfirmationResponse confirmationDetails(String bookingId) {
        return service.confirmationDetails(bookingId);
    }

    @Override
    public CancellationPolicyAfterBookingResponse cancellationPolicyAfterBooking(String bookingId) {
        return service.cancellationPolicyAfterBooking(bookingId);
    }

    @Override
    public CancellationPolicyResponse cancellationPolicy(CancellationPolicyLookupRequest request) {
        return service.cancellationPolicy(request);
    }

    // --- mapping RezLive DTOs → neutral results -------------------------------------------

    private static RateCheckResult toRateCheckResult(PreBookingResponse r) {
        RateCheckResult.RequestEcho echo = null;
        if (r.getPreBookingRequest() != null && r.getPreBookingRequest().getPreBooking() != null) {
            PreBookingResponse.PreBookingEcho pb = r.getPreBookingRequest().getPreBooking();
            List<RateCheckResult.Room> rooms = pb.getRoomDetails() == null ? List.of()
                    : pb.getRoomDetails().stream()
                            .map(er -> new RateCheckResult.Room(er.getType(), er.getBookingKey(), er.getTotalRate()))
                            .toList();
            echo = new RateCheckResult.RequestEcho(new RateCheckResult.PreBooking(
                    rooms, toCancellationInformations(pb.getCancellationInformations())));
        }
        RateCheckResult.PriceDetails details = null;
        if (r.getPreBookingDetails() != null) {
            PreBookingResponse.PreBookingDetails d = r.getPreBookingDetails();
            details = new RateCheckResult.PriceDetails(d.getBookingBeforePrice(), d.getBookingAfterPrice(),
                    d.getDifference(), d.getAgentBalance(), d.getAgentCurrency());
        }
        return new RateCheckResult(echo, details, r.getError());
    }

    private static RateCheckResult.CancellationInformations toCancellationInformations(CancellationInformations ci) {
        if (ci == null) {
            return null;
        }
        List<RateCheckResult.Information> infos = ci.getInformations() == null ? List.of()
                : ci.getInformations().stream()
                        .map(i -> new RateCheckResult.Information(i.getStartDate(), i.getEndDate(),
                                i.getChargeType(), i.getChargeAmount(), i.getCurrency()))
                        .toList();
        return new RateCheckResult.CancellationInformations(infos, ci.getInfo());
    }

    private static CancellationResult toCancellationResult(CancellationResponse r) {
        return new CancellationResult(r.getBookingId(), r.getBookingCode(), r.getStatus(),
                r.getCancellationCharges(), r.getCurrency());
    }
}
