package com.miirphys.bodiala.provider.rezlive.client.dto.booking.book;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response for {@code bookhotel}. We only model {@code <BookingDetails>} (the identifiers + status);
 * the echoed request block is ignored by JAXB.
 */
@XmlRootElement(name = "BookingResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class BookingResponse {

    @XmlElement(name = "BookingDetails")
    private BookingDetails bookingDetails;

    @XmlElement(name = "error")
    private String error;

    public BookingDetails getBookingDetails() {
        return bookingDetails;
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
    public static class BookingDetails {

        @XmlElement(name = "BookingId")
        private String bookingId;
        @XmlElement(name = "BookingCode")
        private String bookingCode;
        @XmlElement(name = "BookingStatus")
        private String bookingStatus;
        @XmlElement(name = "BookingPrice")
        private String bookingPrice;
        @XmlElement(name = "BookingCurrency")
        private String bookingCurrency;

        public String getBookingId() {
            return bookingId;
        }

        public String getBookingCode() {
            return bookingCode;
        }

        public String getBookingStatus() {
            return bookingStatus;
        }

        public String getBookingPrice() {
            return bookingPrice;
        }

        public String getBookingCurrency() {
            return bookingCurrency;
        }
    }
}
