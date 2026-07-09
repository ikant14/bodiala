package com.miirphys.bodiala.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miirphys.bodiala.provider.rezlive.BookingService;
import com.miirphys.bodiala.provider.rezlive.client.RezLiveApiException;
import com.miirphys.bodiala.provider.rezlive.client.RezLiveXmlClient;
import com.miirphys.bodiala.provider.rezlive.client.RezLiveXmlCodec;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.book.BookingRequest;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.book.BookingResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation.CancellationPolicyAfterBookingResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation.CancellationResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.confirmation.HotelConfirmationResponse;
import com.miirphys.bodiala.provider.rezlive.RezLiveProperties;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BookingServiceTest {

    private final RezLiveXmlClient client = mock(RezLiveXmlClient.class);
    private final HotelBookingRepository repo = mock(HotelBookingRepository.class);
    private final RezLiveXmlCodec codec = new RezLiveXmlCodec();
    private final BookingService service = new BookingService(client, withCredentials(), repo);

    private static RezLiveProperties withCredentials() {
        RezLiveProperties props = new RezLiveProperties();
        props.setAgentCode("AG");
        props.setUserName("user");
        props.setApiKey("key");
        return props;
    }

    private static BookRequest bookRequest() {
        return new BookRequest("SESS", "REF1",
                LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 26),
                "AE", "AE", "968", "151754", "Eureka Hotel", "AED",
                List.of(new BookRoom("Standard", "KEY1", "2", "0", "0", "1", "200.00",
                        List.of(new GuestModel("Mr", "Aakash", "Patel", false, null)))));
    }

    private BookingResponse bookingResponse(String id, String code, String status) {
        return codec.unmarshal("<BookingResponse><BookingDetails>"
                + "<BookingId>" + id + "</BookingId><BookingCode>" + code + "</BookingCode>"
                + "<BookingStatus>" + status + "</BookingStatus><BookingPrice>16.51</BookingPrice>"
                + "<BookingCurrency>USD</BookingCurrency></BookingDetails></BookingResponse>", BookingResponse.class);
    }

    @Test
    void refusesWithoutCredentials() {
        BookingService noCreds = new BookingService(client, new RezLiveProperties(), repo);
        assertThatThrownBy(() -> noCreds.book(bookRequest())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void bookConvertsDatesMapsGuestsPersistsAndReturnsStoredBooking() {
        when(client.execute(eq("bookhotel"), any(), eq(BookingResponse.class)))
                .thenReturn(bookingResponse("XHUB560", "XHUBXI-HL-1", "Confirmed"));
        when(repo.save(any(HotelBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        HotelBooking saved = service.book(bookRequest());

        assertThat(saved.getBookingId()).isEqualTo("XHUB560");
        assertThat(saved.getBookingCode()).isEqualTo("XHUBXI-HL-1");
        assertThat(saved.getStatus()).isEqualTo("Confirmed");
        assertThat(saved.getHotelName()).isEqualTo("Eureka Hotel");
        assertThat(saved.getCreatedAt()).isNotNull();
        verify(repo).save(any(HotelBooking.class));

        // The outgoing XML carries dd/MM/yyyy dates and the guest under a <Guests> block.
        ArgumentCaptor<BookingRequest> captor = ArgumentCaptor.forClass(BookingRequest.class);
        verify(client).execute(eq("bookhotel"), captor.capture(), eq(BookingResponse.class));
        String xml = codec.marshal(captor.getValue()).replaceAll("\\s+", "");
        assertThat(xml).contains("<ArrivalDate>25/10/2026</ArrivalDate>");
        assertThat(xml).contains("<AgentRefNo>REF1</AgentRefNo>");
        assertThat(xml).contains("<Guests><Guest><Salutation>Mr</Salutation>");
    }

    @Test
    void rejectsNonConfirmedBookingStatus() {
        when(client.execute(eq("bookhotel"), any(), eq(BookingResponse.class)))
                .thenReturn(bookingResponse("XHUB9", "C9", "Failed"));

        assertThatThrownBy(() -> service.book(bookRequest()))
                .isInstanceOf(RezLiveApiException.class)
                .hasMessageContaining("non-confirmed");
    }

    @Test
    void idempotentBookReturnsExistingForSameAgentRefWithoutChargingAgain() {
        HotelBooking existing = new HotelBooking();
        existing.setBookingId("XHUB-EXISTING");
        existing.setAgentRefNo("REF1");
        when(repo.findByAgentRefNo("REF1")).thenReturn(Optional.of(existing));

        HotelBooking result = service.book(bookRequest()); // agentRefNo REF1

        assertThat(result.getBookingId()).isEqualTo("XHUB-EXISTING");
        verify(client, never()).execute(any(), any(), any());
    }

    @Test
    void rejectsMultiRoomBookRoom() {
        BookRequest multi = new BookRequest("S", "R2",
                LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 26),
                "AE", "AE", "968", "H", "Hotel", "AED",
                List.of(new BookRoom("Std|Std", "K", "2|2", "0|0", "0", "2", "100|100",
                        List.of(new GuestModel("Mr", "A", "B", false, null)))));

        assertThatThrownBy(() -> service.book(multi)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bookErrorResponseThrows() {
        BookingResponse response = new BookingResponse();
        response.setError("Rate no longer available");
        when(client.execute(eq("bookhotel"), any(), eq(BookingResponse.class))).thenReturn(response);

        assertThatThrownBy(() -> service.book(bookRequest()))
                .isInstanceOf(RezLiveApiException.class)
                .hasMessageContaining("Rate no longer available");
    }

    @Test
    void cancelUpdatesStoredStatus() {
        HotelBooking stored = new HotelBooking();
        stored.setBookingId("XHUB560");
        stored.setBookingCode("XHUBXI-HL-1");
        stored.setStatus("Confirmed");
        when(repo.findByBookingId("XHUB560")).thenReturn(Optional.of(stored));
        when(repo.save(any(HotelBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        CancellationResponse cancelResponse = codec.unmarshal(
                "<CancellationResponse><BookingId>XHUB560</BookingId><BookingCode>XHUBXI-HL-1</BookingCode>"
                        + "<Status>true</Status><CancellationCharges>0.00</CancellationCharges>"
                        + "<Currency>USD</Currency></CancellationResponse>", CancellationResponse.class);
        when(client.execute(eq("cancelhotel"), any(), eq(CancellationResponse.class))).thenReturn(cancelResponse);

        CancellationResponse result = service.cancel("XHUB560");

        assertThat(result.isCancelled()).isTrue();
        assertThat(stored.getStatus()).isEqualTo("Cancelled");
        assertThat(stored.getCancelledAt()).isNotNull();
        verify(repo).save(stored);
    }

    @Test
    void cancelUnknownBookingIsNotFound() {
        when(repo.findByBookingId("NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancel("NOPE")).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void confirmationDetailsCallsRightActionForStoredBooking() {
        HotelBooking stored = new HotelBooking();
        stored.setBookingId("XHUB1");
        stored.setBookingCode("C1");
        when(repo.findByBookingId("XHUB1")).thenReturn(Optional.of(stored));
        HotelConfirmationResponse resp = codec.unmarshal(
                "<HotelConfirmationResponse><ConfirmationDetails><HotelConfirmationNo>CN1</HotelConfirmationNo>"
                        + "<ConfirmationStatus>Confirmed</ConfirmationStatus></ConfirmationDetails></HotelConfirmationResponse>",
                HotelConfirmationResponse.class);
        when(client.execute(eq("getConfirmationDetails"), any(), eq(HotelConfirmationResponse.class))).thenReturn(resp);

        HotelConfirmationResponse result = service.confirmationDetails("XHUB1");

        assertThat(result.getConfirmationDetails().getHotelConfirmationNo()).isEqualTo("CN1");
        verify(client).execute(eq("getConfirmationDetails"), any(), eq(HotelConfirmationResponse.class));
    }

    @Test
    void policyAfterBookingCallsRightActionForStoredBooking() {
        HotelBooking stored = new HotelBooking();
        stored.setBookingId("XHUB1");
        stored.setBookingCode("C1");
        when(repo.findByBookingId("XHUB1")).thenReturn(Optional.of(stored));
        CancellationPolicyAfterBookingResponse resp = codec.unmarshal(
                "<CancellationPolicyAfterBookingResponse><CancellationPolicyResponse><CancellationInformations>"
                        + "<Info>policy</Info></CancellationInformations></CancellationPolicyResponse>"
                        + "</CancellationPolicyAfterBookingResponse>",
                CancellationPolicyAfterBookingResponse.class);
        when(client.execute(eq("getCancellationPolicyAfterBooking"), any(),
                eq(CancellationPolicyAfterBookingResponse.class))).thenReturn(resp);

        CancellationPolicyAfterBookingResponse result = service.cancellationPolicyAfterBooking("XHUB1");

        assertThat(result.getCancellationPolicyResponse().getCancellationInformations().getInfo()).isEqualTo("policy");
        verify(client).execute(eq("getCancellationPolicyAfterBooking"), any(),
                eq(CancellationPolicyAfterBookingResponse.class));
    }
}
