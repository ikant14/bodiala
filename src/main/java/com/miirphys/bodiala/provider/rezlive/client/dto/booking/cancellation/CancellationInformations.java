package com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code <CancellationInformations>} wrapper: a list of policy bands plus a free-text
 * {@code <Info>}. Modelled as a class (rather than {@code @XmlElementWrapper}) because the
 * {@code Info} element is a sibling of the repeated bands inside the same wrapper.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CancellationInformations {

    @XmlElement(name = "CancellationInformation")
    private List<CancellationInformation> informations = new ArrayList<>();

    @XmlElement(name = "Info")
    private String info;

    public List<CancellationInformation> getInformations() {
        return informations;
    }

    public String getInfo() {
        return info;
    }
}
