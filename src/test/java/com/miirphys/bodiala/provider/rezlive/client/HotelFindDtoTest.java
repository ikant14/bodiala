package com.miirphys.bodiala.provider.rezlive.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.miirphys.bodiala.provider.rezlive.client.dto.common.Authentication;
import com.miirphys.bodiala.provider.rezlive.client.dto.search.HotelFindRequest;
import com.miirphys.bodiala.provider.rezlive.client.dto.search.HotelFindRequest.Booking;
import com.miirphys.bodiala.provider.rezlive.client.dto.search.HotelFindRequest.Room;
import com.miirphys.bodiala.provider.rezlive.client.dto.search.HotelFindResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class HotelFindDtoTest {

    private final RezLiveXmlCodec codec = new RezLiveXmlCodec();

    @Test
    void marshalsDestinationSearchRequest() {
        Booking booking = new Booking();
        booking.setArrivalDate("25/09/2026");
        booking.setDepartureDate("26/09/2026");
        booking.setCountryCode("AE");
        booking.setCity("968");
        booking.setGuestNationality("AE");
        booking.setHotelRatings(List.of(4, 5));
        Room room = new Room();
        room.setType("Room-1");
        room.setNoOfAdults(1);
        room.setNoOfChilds(0);
        booking.setRooms(List.of(room));

        String xml = codec.marshal(new HotelFindRequest(new Authentication("AG", "user"), booking))
                .replaceAll("\\s+", "");

        assertThat(xml).contains("<HotelFindRequest>");
        assertThat(xml).contains("<Authentication><AgentCode>AG</AgentCode><UserName>user</UserName></Authentication>");
        assertThat(xml).contains("<City>968</City>");
        assertThat(xml).contains("<HotelRatings><HotelRating>4</HotelRating><HotelRating>5</HotelRating></HotelRatings>");
        assertThat(xml).contains("<Rooms><Room><Type>Room-1</Type><NoOfAdults>1</NoOfAdults><NoOfChilds>0</NoOfChilds></Room></Rooms>");
        // No hotel ids and no children-ages wrapper when not applicable.
        assertThat(xml).doesNotContain("<HotelIDs>");
        assertThat(xml).doesNotContain("<ChildrenAges>");
    }

    @Test
    void marshalsSearchByHotelIdsRequest() {
        Booking booking = new Booking();
        booking.setArrivalDate("11/10/2026");
        booking.setDepartureDate("12/10/2026");
        booking.setCountryCode("AE");
        booking.setCity("968");
        booking.setGuestNationality("IN");
        booking.setHotelIds(List.of(150884L, 171888L));
        Room room = new Room();
        room.setType("Room-1");
        room.setNoOfAdults(2);
        room.setNoOfChilds(0);
        booking.setRooms(List.of(room));

        String xml = codec.marshal(new HotelFindRequest(new Authentication("AG", "user"), booking))
                .replaceAll("\\s+", "");

        assertThat(xml).contains("<HotelIDs><Int>150884</Int><Int>171888</Int></HotelIDs>");
    }

    @Test
    void unmarshalsFindResponse() {
        String xml = """
                <?xml version="1.0"?>
                <HotelFindResponse>
                  <SearchSessionId>YWRhc2Rhc2Fkc2FkczEyMjMyc2ZzZA==</SearchSessionId>
                  <ArrivalDate>25/10/2026</ArrivalDate>
                  <DepartureDate>26/10/2026</DepartureDate>
                  <Currency>AED</Currency>
                  <GuestNationality>BE</GuestNationality>
                  <Hotels>
                    <Hotel>
                      <Id>151641</Id>
                      <Name>Shangri-la hotel dubai</Name>
                      <Rating>5.00</Rating>
                      <Price>1249.4664</Price>
                      <RoomDetails>
                        <RoomDetail>
                          <Type>Deluxe Room, Sea View</Type>
                          <BookingKey>RZHNiuMwEIRfRWeBjf4sOzoFhg</BookingKey>
                          <Adults>1</Adults>
                          <Children>0</Children>
                          <ChildrenAges>0</ChildrenAges>
                          <TotalRooms>1</TotalRooms>
                          <TotalRate>1251.07066583</TotalRate>
                          <RoomDescription>Free High-speed Internet</RoomDescription>
                          <BoardBasis>Room Only</BoardBasis>
                          <TermsAndConditions/>
                        </RoomDetail>
                      </RoomDetails>
                    </Hotel>
                  </Hotels>
                </HotelFindResponse>
                """;

        HotelFindResponse response = codec.unmarshal(xml, HotelFindResponse.class);

        assertThat(response.hasError()).isFalse();
        assertThat(response.getSearchSessionId()).isNotBlank();
        assertThat(response.getCurrency()).isEqualTo("AED");
        assertThat(response.getHotels()).hasSize(1);
        HotelFindResponse.FoundHotel hotel = response.getHotels().get(0);
        assertThat(hotel.getId()).isEqualTo("151641");
        assertThat(hotel.getRating()).isEqualTo("5.00");
        assertThat(hotel.getRoomDetails()).hasSize(1);
        assertThat(hotel.getRoomDetails().get(0).getBookingKey()).isEqualTo("RZHNiuMwEIRfRWeBjf4sOzoFhg");
    }
}
