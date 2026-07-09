package com.miirphys.bodiala.provider.rezlive.client.dto.search;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Response for {@code findhotel} / {@code findhotelbyid}. Numeric-looking fields are kept as
 * String because RezLive returns formatted values (e.g. Rating "5.00", and TotalRate for
 * multiple rooms is "|"-separated). On failure an {@code <error>} element is returned.
 */
@XmlRootElement(name = "HotelFindResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class HotelFindResponse {

    @XmlElement(name = "SearchSessionId")
    private String searchSessionId;
    @XmlElement(name = "ArrivalDate")
    private String arrivalDate;
    @XmlElement(name = "DepartureDate")
    private String departureDate;
    @XmlElement(name = "Currency")
    private String currency;
    @XmlElement(name = "GuestNationality")
    private String guestNationality;

    @XmlElementWrapper(name = "Hotels")
    @XmlElement(name = "Hotel")
    private List<FoundHotel> hotels = new ArrayList<>();

    @XmlElement(name = "error")
    private String error;

    public String getSearchSessionId() {
        return searchSessionId;
    }

    public String getArrivalDate() {
        return arrivalDate;
    }

    public String getDepartureDate() {
        return departureDate;
    }

    public String getCurrency() {
        return currency;
    }

    public String getGuestNationality() {
        return guestNationality;
    }

    public List<FoundHotel> getHotels() {
        return hotels;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    /** Not a bean getter ("has" prefix) so it doesn't collide with getError() in JSON output. */
    public boolean hasError() {
        return error != null && !error.isBlank();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FoundHotel {

        @XmlElement(name = "Id")
        private String id;
        @XmlElement(name = "Name")
        private String name;
        @XmlElement(name = "Rating")
        private String rating;
        @XmlElement(name = "Price")
        private String price;

        @XmlElementWrapper(name = "RoomDetails")
        @XmlElement(name = "RoomDetail")
        private List<RoomDetail> roomDetails = new ArrayList<>();

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getRating() {
            return rating;
        }

        public String getPrice() {
            return price;
        }

        public List<RoomDetail> getRoomDetails() {
            return roomDetails;
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
        @XmlElement(name = "RoomDescription")
        private String roomDescription;
        @XmlElement(name = "BoardBasis")
        private String boardBasis;
        @XmlElement(name = "TermsAndConditions")
        private String termsAndConditions;

        public String getType() {
            return type;
        }

        public String getBookingKey() {
            return bookingKey;
        }

        public String getAdults() {
            return adults;
        }

        public String getChildren() {
            return children;
        }

        public String getChildrenAges() {
            return childrenAges;
        }

        public String getTotalRooms() {
            return totalRooms;
        }

        public String getTotalRate() {
            return totalRate;
        }

        public String getRoomDescription() {
            return roomDescription;
        }

        public String getBoardBasis() {
            return boardBasis;
        }

        public String getTermsAndConditions() {
            return termsAndConditions;
        }
    }
}
