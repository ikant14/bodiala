package com.miirphys.bodiala.provider.rezlive.client.dto.booking.book;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Response for {@code getbookingdetails}. A representative subset of the (large) {@code <Booking>}
 * block is modelled; unmapped elements are ignored by JAXB.
 */
@XmlRootElement(name = "GetBookingResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetBookingResponse {

    @XmlElement(name = "Booking")
    private Booking booking;

    @XmlElement(name = "error")
    private String error;

    public Booking getBooking() {
        return booking;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean hasError() {
        return error != null && !error.isBlank();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Booking {

        @XmlElement(name = "BookingId")
        private String bookingId;
        @XmlElement(name = "CheckIn")
        private String checkIn;
        @XmlElement(name = "CheckOut")
        private String checkOut;
        @XmlElement(name = "Bookingdate")
        private String bookingDate;
        @XmlElement(name = "BookingStatus")
        private String bookingStatus;
        @XmlElement(name = "HotelInfo")
        private HotelInfo hotelInfo;
        @XmlElement(name = "RateInfo")
        private RateInfo rateInfo;
        @XmlElement(name = "RoomInfo")
        private RoomInfo roomInfo;

        public String getBookingId() {
            return bookingId;
        }

        public String getCheckIn() {
            return checkIn;
        }

        public String getCheckOut() {
            return checkOut;
        }

        public String getBookingDate() {
            return bookingDate;
        }

        public String getBookingStatus() {
            return bookingStatus;
        }

        public HotelInfo getHotelInfo() {
            return hotelInfo;
        }

        public RateInfo getRateInfo() {
            return rateInfo;
        }

        public RoomInfo getRoomInfo() {
            return roomInfo;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class HotelInfo {

        @XmlElement(name = "HotelId")
        private String hotelId;
        @XmlElement(name = "HotelName")
        private String hotelName;
        @XmlElement(name = "HotelCity")
        private String hotelCity;
        @XmlElement(name = "HotelCountryCode")
        private String hotelCountryCode;
        @XmlElement(name = "LeaderFirstName")
        private String leaderFirstName;
        @XmlElement(name = "LeaderLastName")
        private String leaderLastName;

        public String getHotelId() {
            return hotelId;
        }

        public String getHotelName() {
            return hotelName;
        }

        public String getHotelCity() {
            return hotelCity;
        }

        public String getHotelCountryCode() {
            return hotelCountryCode;
        }

        public String getLeaderFirstName() {
            return leaderFirstName;
        }

        public String getLeaderLastName() {
            return leaderLastName;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RateInfo {

        @XmlElement(name = "CurrencyCode")
        private String currencyCode;
        @XmlElement(name = "TotalRate")
        private String totalRate;

        public String getCurrencyCode() {
            return currencyCode;
        }

        public String getTotalRate() {
            return totalRate;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RoomInfo {

        @XmlElement(name = "RoomType")
        private String roomType;
        @XmlElement(name = "NoOfAdult")
        private String noOfAdult;
        @XmlElement(name = "NoOfChild")
        private String noOfChild;

        @XmlElementWrapper(name = "GuestInfos")
        @XmlElement(name = "GuestInfo")
        private List<GuestInfo> guestInfos = new ArrayList<>();

        public String getRoomType() {
            return roomType;
        }

        public String getNoOfAdult() {
            return noOfAdult;
        }

        public String getNoOfChild() {
            return noOfChild;
        }

        public List<GuestInfo> getGuestInfos() {
            return guestInfos;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GuestInfo {

        @XmlElement(name = "PaxType")
        private String paxType;
        @XmlElement(name = "Salutation")
        private String salutation;
        @XmlElement(name = "FirstName")
        private String firstName;
        @XmlElement(name = "LastName")
        private String lastName;

        public String getPaxType() {
            return paxType;
        }

        public String getSalutation() {
            return salutation;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }
    }
}
