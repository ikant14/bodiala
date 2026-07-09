package com.miirphys.bodiala.provider.rezlive.client.dto.booking.book;

import com.miirphys.bodiala.provider.rezlive.client.dto.common.Authentication;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/** Request for the {@code prebook} action. Echoes a search offer (SearchSessionId + BookingKey). */
@XmlRootElement(name = "PreBookingRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class PreBookingRequest {

    @XmlElement(name = "Authentication")
    private Authentication authentication;

    @XmlElement(name = "PreBooking")
    private PreBooking preBooking;

    public PreBookingRequest() {
    }

    public PreBookingRequest(Authentication authentication, PreBooking preBooking) {
        this.authentication = authentication;
        this.preBooking = preBooking;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PreBooking {

        @XmlElement(name = "SearchSessionId")
        private String searchSessionId;
        @XmlElement(name = "ArrivalDate")
        private String arrivalDate;
        @XmlElement(name = "DepartureDate")
        private String departureDate;
        @XmlElement(name = "GuestNationality")
        private String guestNationality;
        @XmlElement(name = "CountryCode")
        private String countryCode;
        @XmlElement(name = "City")
        private String city;
        @XmlElement(name = "HotelId")
        private String hotelId;
        @XmlElement(name = "Currency")
        private String currency;

        @XmlElementWrapper(name = "RoomDetails")
        @XmlElement(name = "RoomDetail")
        private List<RoomDetail> roomDetails = new ArrayList<>();

        public void setSearchSessionId(String v) {
            this.searchSessionId = v;
        }

        public void setArrivalDate(String v) {
            this.arrivalDate = v;
        }

        public void setDepartureDate(String v) {
            this.departureDate = v;
        }

        public void setGuestNationality(String v) {
            this.guestNationality = v;
        }

        public void setCountryCode(String v) {
            this.countryCode = v;
        }

        public void setCity(String v) {
            this.city = v;
        }

        public void setHotelId(String v) {
            this.hotelId = v;
        }

        public void setCurrency(String v) {
            this.currency = v;
        }

        public void setRoomDetails(List<RoomDetail> v) {
            this.roomDetails = v;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RoomDetail {

        @XmlElement(name = "Type")
        private String type;
        @XmlElement(name = "BookingKey")
        private String bookingKey;
        @XmlElement(name = "Adults")
        private String adults;
        @XmlElement(name = "Children")
        private String children;
        @XmlElement(name = "ChildrenAges")
        private String childrenAges;
        @XmlElement(name = "TotalRooms")
        private String totalRooms;
        @XmlElement(name = "TotalRate")
        private String totalRate;

        public void setType(String v) {
            this.type = v;
        }

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

        public void setTotalRooms(String v) {
            this.totalRooms = v;
        }

        public void setTotalRate(String v) {
            this.totalRate = v;
        }
    }
}
