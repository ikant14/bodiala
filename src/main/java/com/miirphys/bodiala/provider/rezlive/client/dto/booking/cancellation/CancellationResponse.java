package com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Response for {@code cancelhotel}. {@code Status} is a boolean success flag (distinct from BookingStatus). */
@XmlRootElement(name = "CancellationResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class CancellationResponse {

    @XmlElement(name = "BookingId")
    private String bookingId;
    @XmlElement(name = "BookingCode")
    private String bookingCode;
    @XmlElement(name = "Status")
    private String status;
    @XmlElement(name = "CancellationCharges")
    private String cancellationCharges;
    @XmlElement(name = "Currency")
    private String currency;
    @XmlElement(name = "error")
    private String error;

    public String getBookingId() {
        return bookingId;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public String getStatus() {
        return status;
    }

    public String getCancellationCharges() {
        return cancellationCharges;
    }

    public String getCurrency() {
        return currency;
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

    /** True when RezLive reported the cancellation succeeded ({@code <Status>true</Status>}). */
    public boolean isCancelled() {
        return "true".equalsIgnoreCase(status);
    }
}
