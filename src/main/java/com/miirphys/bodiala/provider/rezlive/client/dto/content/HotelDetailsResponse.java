package com.miirphys.bodiala.provider.rezlive.client.dto.content;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Response for the {@code gethoteldetails} action. Each {@code <Hotels>} block describes a
 * single hotel (the element is plural in the RezLive docs). On failure the API returns an
 * {@code <error>} element instead.
 *
 * <p>Note: {@code HotelId} here is alphanumeric (e.g. "XHBE9179"), and City/Country are full
 * names (not codes) — unlike the numeric identifiers and codes used in search and the CSV
 * masters. The CSV {@code HotelCode} to runtime {@code HotelId} relationship is not documented
 * by RezLive and must be verified against live data.
 */
@XmlRootElement(name = "HotelDetailsResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class HotelDetailsResponse {

    @XmlElement(name = "Hotels")
    private List<HotelDetail> hotels = new ArrayList<>();

    @XmlElement(name = "error")
    private String error;

    public List<HotelDetail> getHotels() {
        return hotels;
    }

    public void setHotels(List<HotelDetail> hotels) {
        this.hotels = hotels;
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
    public static class HotelDetail {

        @XmlElement(name = "HotelId")
        private String hotelId;
        @XmlElement(name = "HotelName")
        private String hotelName;
        @XmlElement(name = "Rating")
        private String rating;
        @XmlElement(name = "City")
        private String city;
        @XmlElement(name = "Country")
        private String country;
        @XmlElement(name = "Location")
        private String location;
        @XmlElement(name = "Phone")
        private String phone;
        @XmlElement(name = "Telephone")
        private String telephone;
        @XmlElement(name = "Fax")
        private String fax;
        @XmlElement(name = "Email")
        private String email;
        @XmlElement(name = "Website")
        private String website;
        @XmlElement(name = "Description")
        private String description;
        @XmlElement(name = "HotelAddress")
        private String hotelAddress;
        @XmlElement(name = "Latitude")
        private String latitude;
        @XmlElement(name = "Longitude")
        private String longitude;
        @XmlElement(name = "HotelPostalCode")
        private String hotelPostalCode;
        @XmlElement(name = "HotelAmenities")
        private String hotelAmenities;
        @XmlElement(name = "RoomAmenities")
        private String roomAmenities;

        @XmlElementWrapper(name = "Images")
        @XmlElement(name = "Image")
        private List<String> images = new ArrayList<>();

        public String getHotelId() {
            return hotelId;
        }

        public String getHotelName() {
            return hotelName;
        }

        public String getRating() {
            return rating;
        }

        public String getCity() {
            return city;
        }

        public String getCountry() {
            return country;
        }

        public String getLocation() {
            return location;
        }

        public String getPhone() {
            return phone;
        }

        public String getTelephone() {
            return telephone;
        }

        public String getFax() {
            return fax;
        }

        public String getEmail() {
            return email;
        }

        public String getWebsite() {
            return website;
        }

        public String getDescription() {
            return description;
        }

        public String getHotelAddress() {
            return hotelAddress;
        }

        public String getLatitude() {
            return latitude;
        }

        public String getLongitude() {
            return longitude;
        }

        public String getHotelPostalCode() {
            return hotelPostalCode;
        }

        public String getHotelAmenities() {
            return hotelAmenities;
        }

        public String getRoomAmenities() {
            return roomAmenities;
        }

        public List<String> getImages() {
            return images;
        }
    }
}
