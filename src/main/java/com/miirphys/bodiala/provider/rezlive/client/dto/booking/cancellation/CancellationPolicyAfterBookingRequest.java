package com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation;

import com.miirphys.bodiala.provider.rezlive.client.dto.common.Authentication;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Request for {@code getCancellationPolicyAfterBooking}. Identifiers sit directly under the root. */
@XmlRootElement(name = "CancellationPolicyAfterBookingRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class CancellationPolicyAfterBookingRequest {

    @XmlElement(name = "Authentication")
    private Authentication authentication;
    @XmlElement(name = "BookingId")
    private String bookingId;
    @XmlElement(name = "BookingCode")
    private String bookingCode;

    public CancellationPolicyAfterBookingRequest() {
    }

    public CancellationPolicyAfterBookingRequest(Authentication authentication, String bookingId, String bookingCode) {
        this.authentication = authentication;
        this.bookingId = bookingId;
        this.bookingCode = bookingCode;
    }
}
