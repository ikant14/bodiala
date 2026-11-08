package com.miirphys.bodiala.booking.web;

import com.miirphys.bodiala.booking.BookRequest;
import com.miirphys.bodiala.booking.PrebookRequest;
import com.miirphys.bodiala.config.OpenApiConfig;
import com.miirphys.bodiala.provider.BookingProviderRegistry;
import com.miirphys.bodiala.provider.ProviderId;
import com.miirphys.bodiala.provider.model.CancellationResult;
import com.miirphys.bodiala.provider.model.RateCheckResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The booking chain. New requests (prebook, book) pick a supplier with the optional {@code ?provider=}
 * query param (defaulting to {@code hotel.provider}); operations keyed on an existing booking route
 * back to the supplier that created it, resolved from the stored row's {@code provider} column.
 * Requires the supplier's credentials — 503 otherwise; supplier/transport failures surface as 502;
 * unknown bookingId → 404.
 *
 * <p>Access ({@code security.RouteMatrix}): prebook and book are public; the list is staff-only; read and
 * cancel by bookingId are open until SEC-2 puts them behind the booking's manage token.
 */
@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings (live)", description = "prebook / book / cancel — pick the supplier with ?provider=")
public class BookingController {

    /** The admin list's page-size limit. */
    public static final int MAX_PAGE_SIZE = 100;

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final String PROVIDER_DESC =
            "Supplier to use: hotelbeds (case-insensitive). Omit to use the configured default.";

    private final BookingProviderRegistry bookingProviders;

    public BookingController(BookingProviderRegistry bookingProviders) {
        this.bookingProviders = bookingProviders;
    }

    @Operation(summary = "Pre-book (confirm latest price + cancellation policy)",
            description = "Echo a search offer (per-room BookingKey). Returns a refreshed BookingKey and "
                    + "the price delta; use the refreshed key when booking.")
    @PostMapping("/prebook")
    public RateCheckResult prebook(@RequestBody PrebookRequest request,
                                   @Parameter(description = PROVIDER_DESC)
                                   @RequestParam(required = false) ProviderId provider) {
        return bookingProviders.resolve(provider).prebook(request);
    }

    @Operation(summary = "Book a hotel",
            description = "Confirms the booking and persists it locally (stamped with the chosen provider). "
                    + "Returns the stored booking including the supplier reference.")
    @PostMapping
    public BookingView book(@RequestBody BookRequest request,
                            @Parameter(description = PROVIDER_DESC)
                            @RequestParam(required = false) ProviderId provider) {
        return BookingView.of(bookingProviders.resolve(provider).book(request));
    }

    @Operation(summary = "List every stored booking (staff only, paginated)",
            description = "Newest first. page is 0-based; size is 1-" + MAX_PAGE_SIZE + " (default "
                    + DEFAULT_PAGE_SIZE + "). Requires the staff login (HTTP Basic).")
    @SecurityRequirement(name = OpenApiConfig.STAFF_SCHEME)
    @GetMapping
    public PagedModel<BookingView> list(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be 0 or more.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return new PagedModel<>(bookingProviders.resolve(null).listBookings(pageable).map(BookingView::of));
    }

    @Operation(summary = "Get a locally-stored booking by BookingId")
    @GetMapping("/{bookingId}")
    public BookingView get(@PathVariable String bookingId) {
        return BookingView.of(bookingProviders.forBooking(bookingId).getStored(bookingId));
    }

    @Operation(summary = "Cancel a booking",
            description = "Routes to the supplier that created the booking, cancels, and updates status.")
    @PostMapping("/{bookingId}/cancel")
    public CancellationResult cancel(@PathVariable String bookingId) {
        return bookingProviders.forBooking(bookingId).cancel(bookingId);
    }
}
