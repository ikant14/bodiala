package com.miirphys.bodiala.provider.rezlive.client.dto.booking.book;

import com.miirphys.bodiala.provider.rezlive.client.dto.common.Authentication;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Request for {@code getbookingdetails}. Identifiers sit directly under the root. */
@XmlRootElement(name = "GetBookingRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetBookingRequest {

    @XmlElement(name = "Authentication")
    private Authentication authentication;
    @XmlElement(name = "BookingId")
    private String bookingId;
    @XmlElement(name = "BookingCode")
    private String bookingCode;

    public GetBookingRequest() {
    }

    public GetBookingRequest(Authentication authentication, String bookingId, String bookingCode) {
        this.authentication = authentication;
        this.bookingId = bookingId;
        this.bookingCode = bookingCode;
    }
}
