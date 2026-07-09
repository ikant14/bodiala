package com.miirphys.bodiala.provider.rezlive.client.dto.search;

import com.miirphys.bodiala.provider.rezlive.client.dto.common.Authentication;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * Request for the {@code findhotel} (search by destination) and {@code findhotelbyid} (search by
 * hotel ids) actions — both share the {@code <HotelFindRequest>} root and differ only in the
 * {@code Booking} contents ({@code HotelRatings} for destination search, {@code HotelIDs} for
 * by-id search).
 */
@XmlRootElement(name = "HotelFindRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class HotelFindRequest {

    @XmlElement(name = "Authentication")
    private Authentication authentication;

    @XmlElement(name = "Booking")
    private Booking booking;

    public HotelFindRequest() {
    }

    public HotelFindRequest(Authentication authentication, Booking booking) {
        this.authentication = authentication;
        this.booking = booking;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public Booking getBooking() {
        return booking;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"arrivalDate", "departureDate", "countryCode", "city",
            "guestNationality", "hotelRatings", "hotelIds", "rooms"})
    public static class Booking {

        @XmlElement(name = "ArrivalDate")
        private String arrivalDate;
        @XmlElement(name = "DepartureDate")
        private String departureDate;
        @XmlElement(name = "CountryCode")
        private String countryCode;
        @XmlElement(name = "City")
        private String city;
        @XmlElement(name = "GuestNationality")
        private String guestNationality;

        /** Optional destination-search filter. */
        @XmlElementWrapper(name = "HotelRatings")
        @XmlElement(name = "HotelRating")
        private List<Integer> hotelRatings;

        /** Present only for {@code findhotelbyid}. */
        @XmlElementWrapper(name = "HotelIDs")
        @XmlElement(name = "Int")
        private List<Long> hotelIds;

        @XmlElementWrapper(name = "Rooms")
        @XmlElement(name = "Room")
        private List<Room> rooms = new ArrayList<>();

        public void setArrivalDate(String arrivalDate) {
            this.arrivalDate = arrivalDate;
        }

        public void setDepartureDate(String departureDate) {
            this.departureDate = departureDate;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public void setGuestNationality(String guestNationality) {
            this.guestNationality = guestNationality;
        }

        public void setHotelRatings(List<Integer> hotelRatings) {
            this.hotelRatings = hotelRatings;
        }

        public void setHotelIds(List<Long> hotelIds) {
            this.hotelIds = hotelIds;
        }

        public void setRooms(List<Room> rooms) {
            this.rooms = rooms;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"type", "noOfAdults", "noOfChilds", "childrenAges"})
    public static class Room {

        @XmlElement(name = "Type")
        private String type;
        @XmlElement(name = "NoOfAdults")
        private Integer noOfAdults;
        @XmlElement(name = "NoOfChilds")
        private Integer noOfChilds;

        @XmlElementWrapper(name = "ChildrenAges")
        @XmlElement(name = "ChildAge")
        private List<Integer> childrenAges;

        public void setType(String type) {
            this.type = type;
        }

        public void setNoOfAdults(Integer noOfAdults) {
            this.noOfAdults = noOfAdults;
        }

        public void setNoOfChilds(Integer noOfChilds) {
            this.noOfChilds = noOfChilds;
        }

        public void setChildrenAges(List<Integer> childrenAges) {
            this.childrenAges = childrenAges;
        }
    }
}
