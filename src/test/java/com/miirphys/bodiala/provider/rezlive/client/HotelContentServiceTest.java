package com.miirphys.bodiala.provider.rezlive.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.miirphys.bodiala.provider.rezlive.client.dto.content.HotelDetailsResponse;
import com.miirphys.bodiala.provider.rezlive.RezLiveProperties;
import org.junit.jupiter.api.Test;

class HotelContentServiceTest {

    private RezLiveProperties withCredentials() {
        RezLiveProperties props = new RezLiveProperties();
        props.setAgentCode("AG");
        props.setUserName("user");
        props.setApiKey("key");
        return props;
    }

    @Test
    void refusesWhenCredentialsMissing() {
        HotelContentService service = new HotelContentService(mock(RezLiveXmlClient.class), new RezLiveProperties());

        assertThatThrownBy(() -> service.getHotelDetails("XHUB18"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsEmptyHotelIdList() {
        HotelContentService service = new HotelContentService(mock(RezLiveXmlClient.class), withCredentials());

        assertThatThrownBy(() -> service.getHotelDetails(java.util.List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsWhenRezLiveReturnsErrorBody() {
        RezLiveXmlClient client = mock(RezLiveXmlClient.class);
        HotelDetailsResponse errorResponse = new HotelDetailsResponse();
        errorResponse.setError("Invalid API key / IP not whitelisted");
        when(client.execute(anyString(), any(), eq(HotelDetailsResponse.class))).thenReturn(errorResponse);

        HotelContentService service = new HotelContentService(client, withCredentials());

        assertThatThrownBy(() -> service.getHotelDetails("XHUB18"))
                .isInstanceOf(RezLiveApiException.class)
                .hasMessageContaining("Invalid API key");
    }

    @Test
    void returnsResponseOnSuccess() {
        RezLiveXmlClient client = mock(RezLiveXmlClient.class);
        HotelDetailsResponse ok = new HotelDetailsResponse();
        when(client.execute(anyString(), any(), eq(HotelDetailsResponse.class))).thenReturn(ok);

        HotelContentService service = new HotelContentService(client, withCredentials());

        assertThat(service.getHotelDetails("XHUB18")).isSameAs(ok);
    }
}
