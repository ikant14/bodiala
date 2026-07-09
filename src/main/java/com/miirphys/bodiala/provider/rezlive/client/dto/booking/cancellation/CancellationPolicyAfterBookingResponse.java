package com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response for {@code getCancellationPolicyAfterBooking} — wraps an inner
 * {@code <CancellationPolicyResponse>} identical in shape to the pre-booking policy.
 */
@XmlRootElement(name = "CancellationPolicyAfterBookingResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class CancellationPolicyAfterBookingResponse {

    @XmlElement(name = "CancellationPolicyResponse")
    private CancellationPolicyResponse cancellationPolicyResponse;

    @XmlElement(name = "error")
    private String error;

    public CancellationPolicyResponse getCancellationPolicyResponse() {
        return cancellationPolicyResponse;
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
}
