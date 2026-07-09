package com.miirphys.bodiala.provider.rezlive.client.dto.booking.book;

import com.miirphys.bodiala.provider.rezlive.client.dto.common.Authentication;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/** Request for the {@code bookhotel} action. */
@XmlRootElement(name = "BookingRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class BookingRequest {

    @XmlElement(name = "Authentication")
    private Authentication authentication;

    @XmlElement(name = "Booking")
    private Booking booking;

    public BookingRequest() {
    }

    public BookingRequest(Authentication authentication, Booking booking) {
        this.authentication = authentication;
        this.booking = booking;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Booking {

        @XmlElement(name = "SearchSessionId")
        private String searchSessionId;
        @XmlElement(name = "AgentRefNo")
        private String agentRefNo;
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
        @XmlElement(name = "Name")
        private String name;
        @XmlElement(name = "Currency")
        private String currency;

        @XmlElementWrapper(name = "RoomDetails")
        @XmlElement(name = "RoomDetail")
        private List<RoomDetail> roomDetails = new ArrayList<>();

        public void setSearchSessionId(String v) {
            this.searchSessionId = v;
        }

        public void setAgentRefNo(String v) {
            this.agentRefNo = v;
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

        public void setName(String v) {
            this.name = v;
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

        /** One {@code <Guests>} block per room. */
        @XmlElement(name = "Guests")
        private List<GuestGroup> guests = new ArrayList<>();

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

        public void setGuests(List<GuestGroup> v) {
            this.guests = v;
        }
    }

    /** A {@code <Guests>} block containing the occupants of one room. */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GuestGroup {

        @XmlElement(name = "Guest")
        private List<Guest> guest = new ArrayList<>();

        public GuestGroup() {
        }

        public GuestGroup(List<Guest> guest) {
            this.guest = guest;
        }

        public void setGuest(List<Guest> v) {
            this.guest = v;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Guest {

        @XmlElement(name = "Salutation")
        private String salutation;
        @XmlElement(name = "FirstName")
        private String firstName;
        @XmlElement(name = "LastName")
        private String lastName;
        @XmlElement(name = "IsChild")
        private Integer isChild;
        @XmlElement(name = "Age")
        private Integer age;

        public void setSalutation(String v) {
            this.salutation = v;
        }

        public void setFirstName(String v) {
            this.firstName = v;
        }

        public void setLastName(String v) {
            this.lastName = v;
        }

        public void setIsChild(Integer v) {
            this.isChild = v;
        }

        public void setAge(Integer v) {
            this.age = v;
        }
    }
}
