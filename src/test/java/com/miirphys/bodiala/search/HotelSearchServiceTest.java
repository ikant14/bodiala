package com.miirphys.bodiala.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miirphys.bodiala.provider.rezlive.HotelSearchService;
import com.miirphys.bodiala.provider.rezlive.client.RezLiveApiException;
import com.miirphys.bodiala.provider.rezlive.client.RezLiveXmlClient;
import com.miirphys.bodiala.provider.rezlive.client.RezLiveXmlCodec;
import com.miirphys.bodiala.provider.rezlive.client.dto.search.HotelFindRequest;
import com.miirphys.bodiala.provider.rezlive.client.dto.search.HotelFindResponse;
import com.miirphys.bodiala.provider.rezlive.RezLiveProperties;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class HotelSearchServiceTest {

    private final RezLiveXmlClient client = mock(RezLiveXmlClient.class);
    private final RezLiveXmlCodec codec = new RezLiveXmlCodec();
    private final HotelSearchService service = new HotelSearchService(client, withCredentials());

    private static RezLiveProperties withCredentials() {
        RezLiveProperties props = new RezLiveProperties();
        props.setAgentCode("AG");
        props.setUserName("user");
        props.setApiKey("key");
        return props;
    }

    private static DestinationSearchRequest destinationRequest() {
        return new DestinationSearchRequest(
                LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 26),
                "AE", "968", "AE", List.of(5),
                List.of(new RoomRequest("Room-1", 2, 0, null)));
    }

    @Test
    void refusesWithoutCredentials() {
        HotelSearchService noCreds = new HotelSearchService(client, new RezLiveProperties());
        assertThatThrownBy(() -> noCreds.searchByDestination(destinationRequest()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void destinationSearchRequiresCity() {
        DestinationSearchRequest noCity = new DestinationSearchRequest(
                LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 26),
                "AE", null, "AE", null, List.of(new RoomRequest("Room-1", 1, 0, null)));
        assertThatThrownBy(() -> service.searchByDestination(noCity))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDepartureNotAfterArrival() {
        DestinationSearchRequest badDates = new DestinationSearchRequest(
                LocalDate.of(2026, 9, 26), LocalDate.of(2026, 9, 26),
                "AE", "968", "AE", null, List.of(new RoomRequest("Room-1", 1, 0, null)));
        assertThatThrownBy(() -> service.searchByDestination(badDates))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMismatchedChildrenAges() {
        DestinationSearchRequest twoKidsNoAges = new DestinationSearchRequest(
                LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 26),
                "AE", "968", "AE", null, List.of(new RoomRequest("Room-1", 2, 2, null)));
        assertThatThrownBy(() -> service.searchByDestination(twoKidsNoAges))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void byHotelIdsRequiresIds() {
        HotelIdsSearchRequest noIds = new HotelIdsSearchRequest(
                LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 26),
                "AE", "968", "IN", List.of(), List.of(new RoomRequest("Room-1", 2, 0, null)));
        assertThatThrownBy(() -> service.searchByHotelIds(noIds))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void byHotelIdsRequiresCity() {
        HotelIdsSearchRequest noCity = new HotelIdsSearchRequest(
                LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 26),
                "AE", null, "IN", List.of(150884L), List.of(new RoomRequest("Room-1", 2, 0, null)));
        assertThatThrownBy(() -> service.searchByHotelIds(noCity))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convertsIsoDatesAndCallsFindhotel() {
        when(client.execute(eq("findhotel"), any(), eq(HotelFindResponse.class)))
                .thenReturn(new HotelFindResponse());

        service.searchByDestination(destinationRequest());

        ArgumentCaptor<HotelFindRequest> captor = ArgumentCaptor.forClass(HotelFindRequest.class);
        verify(client).execute(eq("findhotel"), captor.capture(), eq(HotelFindResponse.class));
        String xml = codec.marshal(captor.getValue()).replaceAll("\\s+", "");
        assertThat(xml).contains("<ArrivalDate>25/09/2026</ArrivalDate>");
        assertThat(xml).contains("<DepartureDate>26/09/2026</DepartureDate>");
        assertThat(xml).contains("<City>968</City>");
    }

    @Test
    void byHotelIdsCallsFindhotelbyidWithIds() {
        when(client.execute(eq("findhotelbyid"), any(), eq(HotelFindResponse.class)))
                .thenReturn(new HotelFindResponse());

        service.searchByHotelIds(new HotelIdsSearchRequest(
                LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 12),
                "AE", "968", "IN", List.of(150884L), List.of(new RoomRequest("Room-1", 2, 0, null))));

        ArgumentCaptor<HotelFindRequest> captor = ArgumentCaptor.forClass(HotelFindRequest.class);
        verify(client).execute(eq("findhotelbyid"), captor.capture(), eq(HotelFindResponse.class));
        String xml = codec.marshal(captor.getValue()).replaceAll("\\s+", "");
        assertThat(xml).contains("<HotelIDs><Int>150884</Int></HotelIDs>");
    }

    @Test
    void errorResponseBecomesException() {
        HotelFindResponse error = new HotelFindResponse();
        error.setError("Invalid API key");
        when(client.execute(eq("findhotel"), any(), eq(HotelFindResponse.class))).thenReturn(error);

        assertThatThrownBy(() -> service.searchByDestination(destinationRequest()))
                .isInstanceOf(RezLiveApiException.class)
                .hasMessageContaining("Invalid API key");
    }
}
