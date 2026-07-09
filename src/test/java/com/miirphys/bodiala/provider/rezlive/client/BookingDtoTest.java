package com.miirphys.bodiala.provider.rezlive.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.miirphys.bodiala.provider.rezlive.client.dto.common.Authentication;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.book.BookingRequest;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.book.BookingResponse;
import com.miirphys.bodiala.provider.rezlive.client.dto.booking.book.PreBookingResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookingDtoTest {

    private final RezLiveXmlCodec codec = new RezLiveXmlCodec();

    @Test
    void marshalsBookingRequestWithGuests() {
        BookingRequest.Guest guest = new BookingRequest.Guest();
        guest.setSalutation("Mr");
        guest.setFirstName("Aakash");
        guest.setLastName("Patel");
        BookingRequest.RoomDetail room = new BookingRequest.RoomDetail();
        room.setType("Standard");
        room.setBookingKey("KEY1");
        room.setGuests(List.of(new BookingRequest.GuestGroup(List.of(guest))));
        BookingRequest.Booking booking = new BookingRequest.Booking();
        booking.setSearchSessionId("SESS");
        booking.setAgentRefNo("REF123");
        booking.setHotelId("151754");
        booking.setRoomDetails(List.of(room));

        String xml = codec.marshal(new BookingRequest(new Authentication("AG", "user"), booking))
                .replaceAll("\\s+", "");

        assertThat(xml).contains("<AgentRefNo>REF123</AgentRefNo>");
        assertThat(xml).contains("<Guests><Guest><Salutation>Mr</Salutation><FirstName>Aakash</FirstName><LastName>Patel</LastName></Guest></Guests>");
    }

    @Test
    void unmarshalsBookingResponseDetails() {
        String xml = """
                <?xml version="1.0" ?>
                <BookingResponse>
                  <BookingRequest><Booking><HotelId>265841</HotelId></Booking></BookingRequest>
                  <BookingDetails>
                    <BookingId>XHUB560</BookingId>
                    <BookingCode>XHUBXI-HL-14424443</BookingCode>
                    <BookingStatus>Confirmed</BookingStatus>
                    <BookingPrice>16.51</BookingPrice>
                    <BookingCurrency>USD</BookingCurrency>
                  </BookingDetails>
                </BookingResponse>
                """;

        BookingResponse response = codec.unmarshal(xml, BookingResponse.class);

        assertThat(response.hasError()).isFalse();
        assertThat(response.getBookingDetails().getBookingId()).isEqualTo("XHUB560");
        assertThat(response.getBookingDetails().getBookingCode()).isEqualTo("XHUBXI-HL-14424443");
        assertThat(response.getBookingDetails().getBookingStatus()).isEqualTo("Confirmed");
    }

    @Test
    void unmarshalsPrebookRefreshedKeyAndPriceDelta() {
        String xml = """
                <?xml version="1.0"?>
                <PreBookingResponse>
                  <PreBookingRequest>
                    <PreBooking>
                      <RoomDetails>
                        <RoomDetail>
                          <Type>Double Room</Type>
                          <BookingKey>REFRESHED-KEY-9999</BookingKey>
                          <TotalRate>1079.88</TotalRate>
                        </RoomDetail>
                      </RoomDetails>
                      <CancellationInformations>
                        <CancellationInformation>
                          <StartDate>11 Apr 2026 00:00:00</StartDate>
                          <EndDate>07 Jul 2026 23:59:00</EndDate>
                          <ChargeType>Amount</ChargeType>
                          <ChargeAmount>0</ChargeAmount>
                          <Currency>AED</Currency>
                        </CancellationInformation>
                        <Info>policy text</Info>
                      </CancellationInformations>
                    </PreBooking>
                  </PreBookingRequest>
                  <PreBookingDetails>
                    <BookingBeforePrice>1079.88</BookingBeforePrice>
                    <BookingAfterPrice>1079.88484464</BookingAfterPrice>
                    <Difference>0.0048</Difference>
                    <AgentBalance>240669.45</AgentBalance>
                    <AgentCurrency>USD</AgentCurrency>
                  </PreBookingDetails>
                </PreBookingResponse>
                """;

        PreBookingResponse response = codec.unmarshal(xml, PreBookingResponse.class);

        assertThat(response.hasError()).isFalse();
        assertThat(response.refreshedBookingKeys()).containsExactly("REFRESHED-KEY-9999");
        assertThat(response.getPreBookingDetails().getBookingAfterPrice()).isEqualTo("1079.88484464");
        assertThat(response.getPreBookingRequest().getPreBooking().getCancellationInformations().getInfo())
                .isEqualTo("policy text");
    }
}
