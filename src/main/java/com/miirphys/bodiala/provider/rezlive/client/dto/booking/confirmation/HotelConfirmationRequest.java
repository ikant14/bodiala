package com.miirphys.bodiala.provider.rezlive.client.dto.booking.confirmation;

import com.miirphys.bodiala.provider.rezlive.client.dto.common.Authentication;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Request for the {@code getConfirmationDetails} action. */
@XmlRootElement(name = "HotelConfirmationRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class HotelConfirmationRequest {

    @XmlElement(name = "Authentication")
    private Authentication authentication;

    @XmlElement(name = "Confirmation")
    private Confirmation confirmation;

    public HotelConfirmationRequest() {
    }

    public HotelConfirmationRequest(Authentication authentication, String bookingId, String bookingCode) {
        this.authentication = authentication;
        this.confirmation = new Confirmation(bookingId, bookingCode);
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Confirmation {

        @XmlElement(name = "BookingId")
        private String bookingId;
        @XmlElement(name = "BookingCode")
        private String bookingCode;

        public Confirmation() {
        }

        public Confirmation(String bookingId, String bookingCode) {
            this.bookingId = bookingId;
            this.bookingCode = bookingCode;
        }
    }
}
