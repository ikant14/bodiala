package com.miirphys.bodiala.provider.rezlive.client.dto.content;

import com.miirphys.bodiala.provider.rezlive.client.dto.common.Authentication;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Request for the {@code gethoteldetails} action.
 *
 * <pre>{@code
 * <HotelDetailsRequest>
 *   <Authentication><AgentCode/><UserName/></Authentication>
 *   <Hotels><HotelId>XHUB18</HotelId></Hotels>
 * </HotelDetailsRequest>
 * }</pre>
 */
@XmlRootElement(name = "HotelDetailsRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class HotelDetailsRequest {

    @XmlElement(name = "Authentication")
    private Authentication authentication;

    @XmlElement(name = "Hotels")
    private Hotels hotels;

    public HotelDetailsRequest() {
    }

    public HotelDetailsRequest(Authentication authentication, List<String> hotelIds) {
        this.authentication = authentication;
        this.hotels = new Hotels(hotelIds);
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public Hotels getHotels() {
        return hotels;
    }

    public void setHotels(Hotels hotels) {
        this.hotels = hotels;
    }

    /** Wrapper holding one or more repeated {@code <HotelId>} elements. */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Hotels {

        @XmlElement(name = "HotelId")
        private List<String> hotelIds = new ArrayList<>();

        public Hotels() {
        }

        public Hotels(List<String> hotelIds) {
            if (hotelIds != null) {
                this.hotelIds = new ArrayList<>(hotelIds);
            }
        }

        public List<String> getHotelIds() {
            return hotelIds;
        }

        public void setHotelIds(List<String> hotelIds) {
            this.hotelIds = hotelIds;
        }
    }
}
