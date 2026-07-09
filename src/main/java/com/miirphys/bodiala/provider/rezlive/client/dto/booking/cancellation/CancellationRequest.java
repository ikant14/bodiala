package com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation;

import com.miirphys.bodiala.provider.rezlive.client.dto.common.Authentication;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Request for the {@code cancelhotel} action. */
@XmlRootElement(name = "CancellationRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class CancellationRequest {

    @XmlElement(name = "Authentication")
    private Authentication authentication;

    @XmlElement(name = "Cancellation")
    private Cancellation cancellation;

    public CancellationRequest() {
    }

    public CancellationRequest(Authentication authentication, String bookingId, String bookingCode) {
        this.authentication = authentication;
        this.cancellation = new Cancellation(bookingId, bookingCode);
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Cancellation {

        @XmlElement(name = "BookingId")
        private String bookingId;
        @XmlElement(name = "BookingCode")
        private String bookingCode;

        public Cancellation() {
        }

        public Cancellation(String bookingId, String bookingCode) {
            this.bookingId = bookingId;
            this.bookingCode = bookingCode;
        }
    }
}
