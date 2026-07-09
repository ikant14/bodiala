package com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Response for {@code getcancellationpolicy}. */
@XmlRootElement(name = "CancellationPolicyResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class CancellationPolicyResponse {

    @XmlElement(name = "CancellationInformations")
    private CancellationInformations cancellationInformations;

    @XmlElement(name = "error")
    private String error;

    public CancellationInformations getCancellationInformations() {
        return cancellationInformations;
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
