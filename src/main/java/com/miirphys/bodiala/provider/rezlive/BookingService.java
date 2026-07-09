package com.miirphys.bodiala.provider.rezlive;

import com.miirphys.bodiala.booking.BookRequest;
import com.miirphys.bodiala.booking.BookRoom;
import com.miirphys.bodiala.booking.CancellationPolicyLookupRequest;
import com.miirphys.bodiala.booking.GuestModel;
import com.miirphys.bodiala.booking.HotelBooking;
import com.miirphys.bodiala.booking.HotelBookingRepository;
import com.miirphys.bodiala.booking.PolicyRoom;
import com.miirphys.bodiala.booking.PrebookRequest;
import com.miirphys.bodiala.booking.RoomOffer;
import com.miirphys.bodiala.provider.rezlive.client.RezLiveApiException;
import com.miirphys.bodiala.provider.rezlive.client.RezLiveXmlClient;
import com.miirphys.bodiala.provider.rezlive.client.dto.common.Authentication;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.book.BookingRequest;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.book.BookingResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation.CancellationPolicyRequest;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation.CancellationPolicyResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation.CancellationPolicyAfterBookingRequest;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation.CancellationPolicyAfterBookingResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation.CancellationRequest;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation.CancellationResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.confirmation.HotelConfirmationRequest;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.confirmation.HotelConfirmationResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.book.GetBookingRequest;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.book.GetBookingResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.book.PreBookingRequest;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.book.PreBookingResponse;
import com.miirphys.bodiala.provider.rezlive.RezLiveProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The RezLive booking chain: prebook -> book -> cancel, plus getbookingdetails and a pre-booking
 * cancellation-policy lookup. Bookings are persisted locally on success so the RezLive
 * {@code BookingId}/{@code BookingCode} pair can be reused for downstream calls.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final DateTimeFormatter REZLIVE_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** BookingStatus values we treat as a successful, persistable booking (verify vs sandbox). */
    private static final Set<String> CONFIRMED_STATUSES = Set.of("confirmed", "vouchered");

    private final RezLiveXmlClient client;
    private final RezLiveProperties properties;
    private final HotelBookingRepository bookings;

    public BookingService(RezLiveXmlClient client, RezLiveProperties properties, HotelBookingRepository bookings) {
        this.client = client;
        this.properties = properties;
        this.bookings = bookings;
    }

    public PreBookingResponse prebook(PrebookRequest request) {
        requireCredentials();
        validateDates(request.arrivalDate(), request.departureDate());
        requireRooms(request.rooms());

        PreBookingRequest.PreBooking pb = new PreBookingRequest.PreBooking();
        pb.setSearchSessionId(request.searchSessionId());
        pb.setArrivalDate(fmt(request.arrivalDate()));
        pb.setDepartureDate(fmt(request.departureDate()));
        pb.setGuestNationality(request.guestNationality());
        pb.setCountryCode(request.countryCode());
        pb.setCity(request.city());
        pb.setHotelId(request.hotelId());
        pb.setCurrency(request.currency());
        pb.setRoomDetails(request.rooms().stream().map(BookingService::toPrebookRoom).toList());

        PreBookingRequest xml = new PreBookingRequest(auth(), pb);
        PreBookingResponse response = client.execute("prebook", xml, PreBookingResponse.class);
        if (response.hasError()) {
            throw new RezLiveApiException(response.getError());
        }
        return response;
    }

    /**
     * Books a hotel. Deliberately NOT {@code @Transactional}: the RezLive charge (an irreversible
     * external side effect) must not run inside a DB transaction — that would both pin a pooled
     * connection across the multi-second HTTP call and risk rolling back the only local record of a
     * booking that already succeeded/charged upstream. Persistence happens in its own (repository)
     * transaction after the call.
     */
    public HotelBooking book(BookRequest request) {
        requireCredentials();
        validateDates(request.arrivalDate(), request.departureDate());
        requireRooms(request.rooms());
        validateOneRoomPerBookRoom(request.rooms());

        boolean clientSuppliedRef = request.agentRefNo() != null && !request.agentRefNo().isBlank();
        String agentRefNo = clientSuppliedRef
                ? request.agentRefNo()
                : "BODIALA-" + UUID.randomUUID().toString().substring(0, 8);

        // Idempotency: a retry carrying the same client-supplied agentRefNo returns the existing
        // booking instead of charging again. (A concurrent double-submit within this pre-call window
        // is still possible; a durable reservation-before-call is a go-live design decision.)
        if (clientSuppliedRef) {
            Optional<HotelBooking> existing = bookings.findByAgentRefNo(agentRefNo);
            if (existing.isPresent()) {
                log.info("Idempotent book: returning existing booking {} for agentRefNo {}",
                        existing.get().getBookingId(), agentRefNo);
                return existing.get();
            }
        }

        BookingRequest.Booking booking = new BookingRequest.Booking();
        booking.setSearchSessionId(request.searchSessionId());
        booking.setAgentRefNo(agentRefNo);
        booking.setArrivalDate(fmt(request.arrivalDate()));
        booking.setDepartureDate(fmt(request.departureDate()));
        booking.setGuestNationality(request.guestNationality());
        booking.setCountryCode(request.countryCode());
        booking.setCity(request.city());
        booking.setHotelId(request.hotelId());
        booking.setName(request.hotelName());
        booking.setCurrency(request.currency());
        booking.setRoomDetails(request.rooms().stream().map(BookingService::toBookRoom).toList());

        BookingRequest xml = new BookingRequest(auth(), booking);
        BookingResponse response = client.execute("bookhotel", xml, BookingResponse.class);
        if (response.hasError()) {
            throw new RezLiveApiException(response.getError());
        }
        BookingResponse.BookingDetails details = response.getBookingDetails();
        if (details == null || details.getBookingId() == null) {
            throw new RezLiveApiException("bookhotel returned no BookingDetails/BookingId");
        }
        if (!isConfirmed(details.getBookingStatus())) {
            // Do not persist a Rejected/Failed booking as if it were confirmed.
            throw new RezLiveApiException(
                    "bookhotel returned a non-confirmed BookingStatus: " + details.getBookingStatus());
        }

        HotelBooking stored = new HotelBooking();
        stored.setBookingId(details.getBookingId());
        stored.setBookingCode(details.getBookingCode());
        stored.setStatus(details.getBookingStatus());
        stored.setAgentRefNo(agentRefNo);
        stored.setHotelId(request.hotelId());
        stored.setHotelName(request.hotelName());
        stored.setCity(request.city());
        stored.setCountryCode(request.countryCode());
        stored.setArrivalDate(request.arrivalDate());
        stored.setDepartureDate(request.departureDate());
        stored.setCurrency(details.getBookingCurrency());
        stored.setPrice(details.getBookingPrice());
        stored.setSearchSessionId(request.searchSessionId());
        stored.setCreatedAt(Instant.now());
        try {
            return bookings.save(stored);
        } catch (RuntimeException e) {
            // The upstream booking already succeeded and was charged. Surface the identifiers so the
            // booking is recoverable (cancel/reconcile) even though the local write failed.
            log.error("BOOKING PERSIST FAILED after a successful bookhotel — recover/cancel manually at "
                            + "RezLive: BookingId={} BookingCode={} agentRefNo={}",
                    details.getBookingId(), details.getBookingCode(), agentRefNo, e);
            throw e;
        }
    }

    /**
     * Cancels a booking. Not {@code @Transactional}: the load, the RezLive cancel call, and the
     * status update each run in their own short transaction so the HTTP round-trip never holds a
     * pooled DB connection open.
     */
    public CancellationResponse cancel(String bookingId) {
        requireCredentials();
        HotelBooking stored = requireStored(bookingId);

        CancellationRequest xml = new CancellationRequest(auth(), stored.getBookingId(), stored.getBookingCode());
        CancellationResponse response = client.execute("cancelhotel", xml, CancellationResponse.class);
        if (response.hasError()) {
            throw new RezLiveApiException(response.getError());
        }
        if (response.isCancelled()) {
            stored.setStatus("Cancelled");
            stored.setCancelledAt(Instant.now());
            stored.setCancellationCharges(response.getCancellationCharges());
            bookings.save(stored);
        }
        return response;
    }

    public GetBookingResponse getBookingDetails(String bookingId) {
        requireCredentials();
        HotelBooking stored = requireStored(bookingId);

        GetBookingRequest xml = new GetBookingRequest(auth(), stored.getBookingId(), stored.getBookingCode());
        GetBookingResponse response = client.execute("getbookingdetails", xml, GetBookingResponse.class);
        if (response.hasError()) {
            throw new RezLiveApiException(response.getError());
        }
        return response;
    }

    public HotelConfirmationResponse confirmationDetails(String bookingId) {
        requireCredentials();
        HotelBooking stored = requireStored(bookingId);

        HotelConfirmationRequest xml = new HotelConfirmationRequest(
                auth(), stored.getBookingId(), stored.getBookingCode());
        HotelConfirmationResponse response = client.execute(
                "getConfirmationDetails", xml, HotelConfirmationResponse.class);
        if (response.hasError()) {
            throw new RezLiveApiException(response.getError());
        }
        return response;
    }

    public CancellationPolicyAfterBookingResponse cancellationPolicyAfterBooking(String bookingId) {
        requireCredentials();
        HotelBooking stored = requireStored(bookingId);

        CancellationPolicyAfterBookingRequest xml = new CancellationPolicyAfterBookingRequest(
                auth(), stored.getBookingId(), stored.getBookingCode());
        CancellationPolicyAfterBookingResponse response = client.execute(
                "getCancellationPolicyAfterBooking", xml, CancellationPolicyAfterBookingResponse.class);
        if (response.hasError()) {
            throw new RezLiveApiException(response.getError());
        }
        return response;
    }

    public CancellationPolicyResponse cancellationPolicy(CancellationPolicyLookupRequest request) {
        requireCredentials();
        validateDates(request.arrivalDate(), request.departureDate());
        if (request.rooms() == null || request.rooms().isEmpty()) {
            throw new IllegalArgumentException("at least one room is required");
        }

        CancellationPolicyRequest xml = new CancellationPolicyRequest();
        xml.setAuthentication(auth());
        xml.setArrivalDate(fmt(request.arrivalDate()));
        xml.setDepartureDate(fmt(request.departureDate()));
        xml.setHotelId(request.hotelId());
        xml.setCountryCode(request.countryCode());
        xml.setCity(request.city());
        xml.setGuestNationality(request.guestNationality());
        xml.setCurrency(request.currency());
        xml.setRoomDetails(request.rooms().stream().map(BookingService::toPolicyRoom).toList());

        CancellationPolicyResponse response = client.execute(
                "getcancellationpolicy", xml, CancellationPolicyResponse.class);
        if (response.hasError()) {
            throw new RezLiveApiException(response.getError());
        }
        return response;
    }

    public List<HotelBooking> listBookings() {
        return bookings.findAll();
    }

    public HotelBooking getStored(String bookingId) {
        return requireStored(bookingId);
    }

    // --- mapping ---------------------------------------------------------------------------

    private static PreBookingRequest.RoomDetail toPrebookRoom(RoomOffer offer) {
        PreBookingRequest.RoomDetail room = new PreBookingRequest.RoomDetail();
        room.setType(offer.type());
        room.setBookingKey(offer.bookingKey());
        room.setAdults(offer.adults());
        room.setChildren(offer.children());
        room.setChildrenAges(offer.childrenAges());
        room.setTotalRooms(offer.totalRooms());
        room.setTotalRate(offer.totalRate());
        return room;
    }

    private static BookingRequest.RoomDetail toBookRoom(BookRoom source) {
        BookingRequest.RoomDetail room = new BookingRequest.RoomDetail();
        room.setType(source.type());
        room.setBookingKey(source.bookingKey());
        room.setAdults(source.adults());
        room.setChildren(source.children());
        room.setChildrenAges(source.childrenAges());
        room.setTotalRooms(source.totalRooms());
        room.setTotalRate(source.totalRate());
        List<BookingRequest.Guest> guests = (source.guests() == null ? List.<GuestModel>of() : source.guests())
                .stream().map(BookingService::toGuest).toList();
        room.setGuests(List.of(new BookingRequest.GuestGroup(guests)));
        return room;
    }

    private static BookingRequest.Guest toGuest(GuestModel model) {
        BookingRequest.Guest guest = new BookingRequest.Guest();
        guest.setSalutation(model.salutation());
        guest.setFirstName(model.firstName());
        guest.setLastName(model.lastName());
        if (model.isChild()) {
            guest.setIsChild(1);
            guest.setAge(model.age());
        }
        return guest;
    }

    private static CancellationPolicyRequest.RoomDetail toPolicyRoom(PolicyRoom source) {
        CancellationPolicyRequest.RoomDetail room = new CancellationPolicyRequest.RoomDetail();
        room.setBookingKey(source.bookingKey());
        room.setAdults(source.adults());
        room.setChildren(source.children());
        room.setChildrenAges(source.childrenAges());
        room.setType(source.type());
        return room;
    }

    // --- helpers ---------------------------------------------------------------------------

    private Authentication auth() {
        return new Authentication(properties.getAgentCode(), properties.getUserName());
    }

    private HotelBooking requireStored(String bookingId) {
        return bookings.findByBookingId(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Unknown bookingId " + bookingId));
    }

    private static String fmt(LocalDate date) {
        return date.format(REZLIVE_DATE);
    }

    private void validateDates(LocalDate arrival, LocalDate departure) {
        if (arrival == null || departure == null) {
            throw new IllegalArgumentException("arrivalDate and departureDate are required");
        }
        if (!departure.isAfter(arrival)) {
            throw new IllegalArgumentException("departureDate must be after arrivalDate");
        }
    }

    private void requireRooms(List<?> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            throw new IllegalArgumentException("at least one room is required");
        }
    }

    private static boolean isConfirmed(String status) {
        return status != null && CONFIRMED_STATUSES.contains(status.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * RezLive packs multiple rooms into one {@code <RoomDetail>} via pipe-delimited fields and one
     * {@code <Guests>} block per room. Our mapper emits a single {@code <Guests>} block per BookRoom,
     * so a BookRoom must describe exactly one physical room — reject pipe-delimited occupancy or
     * TotalRooms>1 rather than send a malformed request. (Multi-room-per-RoomDetail support is a
     * follow-up that needs sandbox verification.)
     */
    private void validateOneRoomPerBookRoom(List<BookRoom> rooms) {
        for (BookRoom room : rooms) {
            boolean piped = hasPipe(room.adults()) || hasPipe(room.children())
                    || hasPipe(room.totalRate()) || hasPipe(room.type());
            boolean multiRoom = parseIntOrOne(room.totalRooms()) > 1;
            if (piped || multiRoom) {
                throw new IllegalArgumentException(
                        "Each BookRoom must describe a single room; pipe-delimited occupancy or "
                                + "totalRooms>1 is not supported — send one room per BookRoom entry.");
            }
        }
    }

    private static boolean hasPipe(String s) {
        return s != null && s.contains("|");
    }

    private static int parseIntOrOne(String s) {
        if (s == null || s.isBlank()) {
            return 1;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void requireCredentials() {
        if (!properties.hasCredentials()) {
            throw new IllegalStateException(
                    "RezLive credentials are not configured (rezlive.agent-code / rezlive.user-name / "
                            + "rezlive.api-key). Cannot book until credentials and IP whitelisting are in place.");
        }
    }
}
