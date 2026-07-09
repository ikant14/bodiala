package com.miirphys.bodiala.provider.rezlive.client;

import com.miirphys.bodiala.provider.rezlive.client.dto.common.Authentication;
import com.miirphys.bodiala.provider.rezlive.client.dto.content.HotelDetailsRequest;
import com.miirphys.bodiala.provider.rezlive.client.dto.content.HotelDetailsResponse;
import com.miirphys.bodiala.provider.rezlive.RezLiveProperties;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Fetches live per-hotel static content via the RezLive {@code gethoteldetails} action.
 *
 * <p>This is the one part of RezLive's "static data" that is pulled over the API (the bulk
 * master files are CSV downloads). Requires configured credentials + a whitelisted IP.
 */
@Service
public class HotelContentService {

    private final RezLiveXmlClient client;
    private final RezLiveProperties properties;

    public HotelContentService(RezLiveXmlClient client, RezLiveProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public HotelDetailsResponse getHotelDetails(String hotelId) {
        return getHotelDetails(List.of(hotelId));
    }

    public HotelDetailsResponse getHotelDetails(List<String> hotelIds) {
        if (!properties.hasCredentials()) {
            throw new IllegalStateException(
                    "RezLive credentials are not configured (rezlive.agent-code / rezlive.user-name / "
                            + "rezlive.api-key). Cannot call gethoteldetails until credentials and IP "
                            + "whitelisting are in place.");
        }
        if (hotelIds == null || hotelIds.isEmpty()) {
            throw new IllegalArgumentException("At least one hotelId is required");
        }
        HotelDetailsRequest request = new HotelDetailsRequest(
                new Authentication(properties.getAgentCode(), properties.getUserName()), hotelIds);
        HotelDetailsResponse response = client.execute("gethoteldetails", request, HotelDetailsResponse.class);
        // RezLive signals auth/IP failures as a 200 with an <error> body — surface it as an error
        // rather than returning an empty, success-looking response.
        if (response.hasError()) {
            throw new RezLiveApiException(response.getError());
        }
        return response;
    }
}
