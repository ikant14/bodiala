package com.miirphys.bodiala.booking.web;

import com.miirphys.bodiala.booking.BookRequest;
import com.miirphys.bodiala.booking.CancellationPolicyLookupRequest;
import com.miirphys.bodiala.booking.PrebookRequest;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation.CancellationPolicyAfterBookingResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation.CancellationPolicyResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.book.GetBookingResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.confirmation.HotelConfirmationResponse;
import com.miirphys.bodiala.provider.BookingProviderRegistry;
import com.miirphys.bodiala.provider.ProviderId;
import com.miirphys.bodiala.provider.model.CancellationResult;
import com.miirphys.bodiala.provider.model.RateCheckResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The booking chain. Both suppliers are loaded: requests that open a new interaction (prebook, book,
 * the pre-booking cancellation policy) pick a supplier with the optional {@code ?provider=} query
 * param (defaulting to {@code hotel.provider}); operations keyed on an existing booking route back to
 * the supplier that created it, resolved from the stored row's {@code provider} column. Requires the
 * supplier's credentials (+ a whitelisted IP for RezLive) — 503 otherwise; supplier/transport
 * failures surface as 502; unknown bookingId → 404; ops a supplier hasn't implemented yet → 501.
 */
@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings (live)", description = "prebook / book / cancel / details — pick the supplier with ?provider=")
public class BookingController {

    private static final String PROVIDER_DESC =
            "Supplier to use: rezlive or hotelbeds (case-insensitive). Omit to use the configured default.";

    private final BookingProviderRegistry bookingProviders;

    public BookingController(BookingProviderRegistry bookingProviders) {
        this.bookingProviders = bookingProviders;
    }

    @Operation(summary = "Pre-book (confirm latest price + cancellation policy)",
            description = "Echo a search offer (SearchSessionId + per-room BookingKey). Returns a refreshed "
                    + "BookingKey and the price delta; use the refreshed key when booking.")
    @PostMapping("/prebook")
    public RateCheckResult prebook(@RequestBody PrebookRequest request,
                                   @Parameter(description = PROVIDER_DESC)
                                   @RequestParam(required = false) ProviderId provider) {
        return bookingProviders.resolve(provider).prebook(request);
    }

    @Operation(summary = "Book a hotel",
            description = "Confirms the booking and persists it locally (stamped with the chosen provider). "
                    + "Returns the stored booking including the supplier BookingId + BookingCode.")
    @PostMapping
    public BookingView book(@RequestBody BookRequest request,
                            @Parameter(description = PROVIDER_DESC)
                            @RequestParam(required = false) ProviderId provider) {
        return BookingView.of(bookingProviders.resolve(provider).book(request));
    }

    @Operation(summary = "List locally-stored bookings (all suppliers)")
    @GetMapping
    public List<BookingView> list() {
        return bookingProviders.resolve(null).listBookings().stream().map(BookingView::of).toList();
    }

    @Operation(summary = "Get a locally-stored booking by BookingId")
    @GetMapping("/{bookingId}")
    public BookingView get(@PathVariable String bookingId) {
        return BookingView.of(bookingProviders.forBooking(bookingId).getStored(bookingId));
    }

    @Operation(summary = "Fetch live booking details from the supplier (getbookingdetails)")
    @GetMapping("/{bookingId}/details")
    public GetBookingResponse details(@PathVariable String bookingId) {
        return bookingProviders.forBooking(bookingId).getBookingDetails(bookingId);
    }

    @Operation(summary = "Cancel a booking (cancelhotel)",
            description = "Routes to the supplier that created the booking, cancels, and updates status.")
    @PostMapping("/{bookingId}/cancel")
    public CancellationResult cancel(@PathVariable String bookingId) {
        return bookingProviders.forBooking(bookingId).cancel(bookingId);
    }

    @Operation(summary = "Pre-booking cancellation policy (getcancellationpolicy)",
            description = "Policy for a search offer (keyed on BookingKey), before any booking exists.")
    @PostMapping("/cancellation-policy")
    public CancellationPolicyResponse cancellationPolicy(@RequestBody CancellationPolicyLookupRequest request,
                                                         @Parameter(description = PROVIDER_DESC)
                                                         @RequestParam(required = false) ProviderId provider) {
        return bookingProviders.resolve(provider).cancellationPolicy(request);
    }

    @Operation(summary = "Supplier confirmation details (getConfirmationDetails)",
            description = "Hotel-side confirmation number / status for a booking.")
    @GetMapping("/{bookingId}/confirmation")
    public HotelConfirmationResponse confirmation(@PathVariable String bookingId) {
        return bookingProviders.forBooking(bookingId).confirmationDetails(bookingId);
    }

    @Operation(summary = "Post-booking cancellation policy (getCancellationPolicyAfterBooking)",
            description = "Cancellation policy for an existing booking (keyed on BookingId + BookingCode).")
    @GetMapping("/{bookingId}/cancellation-policy")
    public CancellationPolicyAfterBookingResponse cancellationPolicyAfterBooking(@PathVariable String bookingId) {
        return bookingProviders.forBooking(bookingId).cancellationPolicyAfterBooking(bookingId);
    }
}
