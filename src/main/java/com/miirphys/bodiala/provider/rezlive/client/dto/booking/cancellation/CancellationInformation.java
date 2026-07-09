package com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * One cancellation-policy band: charge applied for cancelling between {@code StartDate} and
 * {@code EndDate}. Shared across prebook / getcancellationpolicy / after-booking responses.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CancellationInformation {

    @XmlElement(name = "StartDate")
    private String startDate;
    @XmlElement(name = "EndDate")
    private String endDate;
    @XmlElement(name = "ChargeType")
    private String chargeType;
    @XmlElement(name = "ChargeAmount")
    private String chargeAmount;
    @XmlElement(name = "Currency")
    private String currency;

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getChargeType() {
        return chargeType;
    }

    public String getChargeAmount() {
        return chargeAmount;
    }

    public String getCurrency() {
        return currency;
    }
}
