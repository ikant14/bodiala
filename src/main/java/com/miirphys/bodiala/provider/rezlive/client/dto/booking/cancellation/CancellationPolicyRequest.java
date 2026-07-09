package com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation;

import com.miirphys.bodiala.provider.rezlive.client.dto.common.Authentication;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Request for {@code getcancellationpolicy} (pre-booking check). Keyed on per-room BookingKey;
 * fields sit directly under the root (no Booking/PreBooking wrapper).
 */
@XmlRootElement(name = "CancellationPolicyRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class CancellationPolicyRequest {

    @XmlElement(name = "Authentication")
    private Authentication authentication;
    @XmlElement(name = "ArrivalDate")
    private String arrivalDate;
    @XmlElement(name = "DepartureDate")
    private String departureDate;
    @XmlElement(name = "HotelId")
    private String hotelId;
    @XmlElement(name = "CountryCode")
    private String countryCode;
    @XmlElement(name = "City")
    private String city;
    @XmlElement(name = "GuestNationality")
    private String guestNationality;
    @XmlElement(name = "Currency")
    private String currency;

    @XmlElementWrapper(name = "RoomDetails")
    @XmlElement(name = "RoomDetail")
    private List<RoomDetail> roomDetails = new ArrayList<>();

    public CancellationPolicyRequest() {
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication v) {
        this.authentication = v;
    }

    public void setArrivalDate(String v) {
        this.arrivalDate = v;
    }

    public void setDepartureDate(String v) {
        this.departureDate = v;
    }

    public void setHotelId(String v) {
        this.hotelId = v;
    }

    public void setCountryCode(String v) {
        this.countryCode = v;
    }

    public void setCity(String v) {
        this.city = v;
    }

    public void setGuestNationality(String v) {
        this.guestNationality = v;
    }

    public void setCurrency(String v) {
        this.currency = v;
    }

    public void setRoomDetails(List<RoomDetail> v) {
        this.roomDetails = v;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RoomDetail {

        @XmlElement(name = "BookingKey")
        private String bookingKey;
        @XmlElement(name = "Adults")
        private String adults;
        @XmlElement(name = "Children")
        private String children;
        @XmlElement(name = "ChildrenAges")
        private String childrenAges;
        @XmlElement(name = "Type")
        private String type;

        public void setBookingKey(String v) {
            this.bookingKey = v;
        }

        public void setAdults(String v) {
            this.adults = v;
        }

        public void setChildren(String v) {
            this.children = v;
        }

        public void setChildrenAges(String v) {
            this.childrenAges = v;
        }

        public void setType(String v) {
            this.type = v;
        }
    }
}
