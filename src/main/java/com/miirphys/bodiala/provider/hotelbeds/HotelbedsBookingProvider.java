package com.miirphys.bodiala.provider.hotelbeds;

import com.miirphys.bodiala.booking.BookRequest;
import com.miirphys.bodiala.booking.BookRoom;
import com.miirphys.bodiala.booking.CancellationPolicyLookupRequest;
import com.miirphys.bodiala.booking.GuestModel;
import com.miirphys.bodiala.booking.HotelBooking;
import com.miirphys.bodiala.booking.HotelBookingRepository;
import com.miirphys.bodiala.booking.PrebookRequest;
import com.miirphys.bodiala.provider.BookingProvider;
import com.miirphys.bodiala.provider.ProviderId;
import com.miirphys.bodiala.provider.error.UpstreamApiException;
import com.miirphys.bodiala.provider.hotelbeds.dto.BookingResponse;
import com.miirphys.bodiala.provider.hotelbeds.dto.CheckRateResponse;
import com.miirphys.bodiala.provider.model.CancellationResult;
import com.miirphys.bodiala.provider.model.RateCheckResult;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.book.GetBookingResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation.CancellationPolicyAfterBookingResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation.CancellationPolicyResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.confirmation.HotelConfirmationResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Hotelbeds-backed {@link BookingProvider}. The core flow is implemented against APItude:
 * {@code prebook} → {@code POST /checkrates} (always, for a fresh rateKey + price), {@code book} →
 * {@code POST /bookings}, {@code cancel} → {@code DELETE /bookings/{ref}}. Persistence uses the
 * shared {@link HotelBooking} table (stamped {@code provider=HOTELBEDS}); {@code book}/{@code cancel}
 * are deliberately NOT {@code @Transactional}. The four secondary reads are not yet mapped for
 * Hotelbeds and throw {@link UnsupportedOperationException} (→ 501). Always loaded and registered
 * under {@link ProviderId#HOTELBEDS}; new requests select it via {@code ?provider=hotelbeds}, while
 * operations on an existing booking route here automatically when the stored row's {@code provider}
 * is HOTELBEDS.
 */
@Component
public class HotelbedsBookingProvider implements BookingProvider {

    private static final Logger log = LoggerFactory.getLogger(HotelbedsBookingProvider.class);

    private final HotelbedsJsonClient client;
    private final HotelbedsProperties properties;
    private final HotelBookingRepository bookings;

    public HotelbedsBookingProvider(HotelbedsJsonClient client, HotelbedsProperties properties,
                                    HotelBookingRepository bookings) {
        this.client = client;
        this.properties = properties;
        this.bookings = bookings;
    }

    @Override
    public ProviderId id() {
        return ProviderId.HOTELBEDS;
    }

    @Override
    public boolean isConfigured() {
        return properties.hasCredentials();
    }

    @Override
    public RateCheckResult prebook(PrebookRequest request) {
        requireCredentials();
        if (request.rooms() == null || request.rooms().isEmpty()) {
            throw new IllegalArgumentException("at least one room is required");
        }
        List<Map<String, Object>> rooms = request.rooms().stream()
                .map(o -> Map.<String, Object>of("rateKey", rateKey(o.bookingKey())))
                .toList();
        CheckRateResponse response = client.post("/hotel-api/1.0/checkrates",
                Map.of("rooms", rooms), CheckRateResponse.class);
        return HotelbedsBookingMapper.toRateCheckResult(response);
    }

    /**
     * Books via {@code POST /bookings}. NOT {@code @Transactional} (the charge must not run inside a
     * DB tx). Best-effort idempotency: a retry with the same client-supplied {@code agentRefNo}
     * returns the existing row.
     */
    @Override
    public HotelBooking book(BookRequest request) {
        requireCredentials();
        validateDates(request);
        if (request.rooms() == null || request.rooms().isEmpty()) {
            throw new IllegalArgumentException("at least one room is required");
        }

        boolean clientSuppliedRef = request.agentRefNo() != null && !request.agentRefNo().isBlank();
        String clientReference = clientReference(request.agentRefNo());
        if (clientSuppliedRef) {
            Optional<HotelBooking> existing = bookings.findByAgentRefNo(clientReference);
            if (existing.isPresent()) {
                log.info("Idempotent book: returning existing Hotelbeds booking {} for clientReference {}",
                        existing.get().getBookingId(), clientReference);
                return existing.get();
            }
        }

        GuestModel lead = leadGuest(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("holder", Map.of(
                "name", lead != null && lead.firstName() != null ? lead.firstName() : "Guest",
                "surname", lead != null && lead.lastName() != null ? lead.lastName() : "Guest"));
        body.put("rooms", buildRooms(request.rooms()));
        body.put("clientReference", clientReference);
        body.put("tolerance", 2);

        BookingResponse response = client.post("/hotel-api/1.0/bookings", body, BookingResponse.class);
        BookingResponse.Booking b = response == null ? null : response.booking();
        if (b == null || b.reference() == null) {
            throw new UpstreamApiException("Hotelbeds bookings call returned no booking reference");
        }
        if (!isBookable(b.status())) {
            throw new UpstreamApiException("Hotelbeds booking not confirmed: " + b.status());
        }

        HotelBooking stored = new HotelBooking();
        stored.setProvider(ProviderId.HOTELBEDS.name());
        stored.setBookingId(b.reference());          // single Hotelbeds reference; no separate BookingCode
        stored.setStatus(titleCase(b.status()));
        stored.setAgentRefNo(clientReference);
        stored.setHotelId(request.hotelId());
        stored.setHotelName(request.hotelName());
        stored.setCity(request.city());
        stored.setCountryCode(request.countryCode());
        stored.setArrivalDate(request.arrivalDate());
        stored.setDepartureDate(request.departureDate());
        stored.setCurrency(b.currency());
        stored.setPrice(b.hotel() != null ? b.hotel().totalNet() : null);
        stored.setSearchSessionId(request.searchSessionId());
        stored.setCreatedAt(Instant.now());
        try {
            return bookings.save(stored);
        } catch (RuntimeException e) {
            log.error("HOTELBEDS BOOKING PERSIST FAILED after a successful bookings call — recover/cancel "
                    + "manually at Hotelbeds: reference={} clientReference={}", b.reference(), clientReference, e);
            throw e;
        }
    }

    /** Cancels via {@code DELETE /bookings/{ref}}. Not {@code @Transactional} (HTTP call outside any DB tx). */
    @Override
    public CancellationResult cancel(String bookingId) {
        requireCredentials();
        HotelBooking stored = getStored(bookingId);

        BookingResponse response = client.delete(
                "/hotel-api/1.0/bookings/" + stored.getBookingId() + "?cancellationFlag=CANCELLATION",
                BookingResponse.class);
        BookingResponse.Booking b = response == null ? null : response.booking();
        String status = b == null ? null : b.status();
        String charges = (b != null && b.hotel() != null && b.hotel().cancellationAmount() != null)
                ? b.hotel().cancellationAmount().toPlainString() : null;

        if ("CANCELLED".equalsIgnoreCase(status)) {
            stored.setStatus("Cancelled");
            stored.setCancelledAt(Instant.now());
            stored.setCancellationCharges(charges);
            bookings.save(stored);
        }
        return new CancellationResult(stored.getBookingId(), stored.getBookingCode(),
                status, charges, stored.getCurrency());
    }

    @Override
    public List<HotelBooking> listBookings() {
        return bookings.findAll();
    }

    @Override
    public HotelBooking getStored(String bookingId) {
        return bookings.findByBookingId(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Unknown bookingId " + bookingId));
    }

    // --- secondary reads: not yet mapped for Hotelbeds (→ 501) -----------------------------

    @Override
    public GetBookingResponse getBookingDetails(String bookingId) {
        throw notMapped("getBookingDetails");
    }

    @Override
    public HotelConfirmationResponse confirmationDetails(String bookingId) {
        throw notMapped("confirmationDetails");
    }

    @Override
    public CancellationPolicyAfterBookingResponse cancellationPolicyAfterBooking(String bookingId) {
        throw notMapped("cancellationPolicyAfterBooking");
    }

    @Override
    public CancellationPolicyResponse cancellationPolicy(CancellationPolicyLookupRequest request) {
        throw notMapped("cancellationPolicy");
    }

    // --- helpers ---------------------------------------------------------------------------

    private List<Map<String, Object>> buildRooms(List<BookRoom> src) {
        List<Map<String, Object>> rooms = new ArrayList<>();
        for (int i = 0; i < src.size(); i++) {
            BookRoom room = src.get(i);
            int roomId = i + 1;
            List<GuestModel> guests = room.guests() == null ? List.of() : room.guests();
            List<Map<String, Object>> paxes = guests.stream().map(g -> pax(roomId, g)).toList();
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("rateKey", rateKey(room.bookingKey()));
            r.put("paxes", paxes);
            rooms.add(r);
        }
        return rooms;
    }

    private static Map<String, Object> pax(int roomId, GuestModel g) {
        Map<String, Object> pax = new LinkedHashMap<>();
        pax.put("roomId", roomId);
        pax.put("type", g.isChild() ? "CH" : "AD");
        if (g.firstName() != null) {
            pax.put("name", g.firstName());
        }
        if (g.lastName() != null) {
            pax.put("surname", g.lastName());
        }
        if (g.isChild() && g.age() != null) {
            pax.put("age", g.age());
        }
        return pax;
    }

    private static GuestModel leadGuest(BookRequest request) {
        GuestModel firstAny = null;
        for (BookRoom room : request.rooms()) {
            if (room.guests() == null) {
                continue;
            }
            for (GuestModel g : room.guests()) {
                if (firstAny == null) {
                    firstAny = g;
                }
                if (!g.isChild()) {
                    return g;
                }
            }
        }
        return firstAny;
    }

    /** Strip our {@code #rateType} suffix to recover the raw Hotelbeds rateKey for the wire. */
    private static String rateKey(String bookingKey) {
        if (bookingKey == null) {
            return null;
        }
        int hash = bookingKey.lastIndexOf('#');
        return hash < 0 ? bookingKey : bookingKey.substring(0, hash);
    }

    /** clientReference must be 1..20 chars; store exactly what we send (idempotency + reconcile). */
    private static String clientReference(String agentRefNo) {
        String ref = (agentRefNo != null && !agentRefNo.isBlank())
                ? agentRefNo : "BODIALA-" + UUID.randomUUID().toString().substring(0, 8);
        return ref.length() > 20 ? ref.substring(0, 20) : ref;
    }

    private static boolean isBookable(String status) {
        return status != null
                && (status.equalsIgnoreCase("CONFIRMED") || status.equalsIgnoreCase("PRECONFIRMED"));
    }

    private static String titleCase(String s) {
        if (s == null || s.isBlank()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    private void validateDates(BookRequest request) {
        if (request.arrivalDate() == null || request.departureDate() == null) {
            throw new IllegalArgumentException("arrivalDate and departureDate are required");
        }
        if (!request.departureDate().isAfter(request.arrivalDate())) {
            throw new IllegalArgumentException("departureDate must be after arrivalDate");
        }
    }

    private void requireCredentials() {
        if (!properties.hasCredentials()) {
            throw new IllegalStateException(
                    "Hotelbeds credentials are not configured (hotelbeds.api-key / hotelbeds.secret).");
        }
    }

    private static UnsupportedOperationException notMapped(String op) {
        return new UnsupportedOperationException(op + " is not yet supported for the Hotelbeds provider");
    }
}
