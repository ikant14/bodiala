package com.miirphys.bodiala.provider.rezlive.client.dto.booking.confirmation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Response for {@code getConfirmationDetails} — supplier/hotel-side confirmation info. */
@XmlRootElement(name = "HotelConfirmationResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class HotelConfirmationResponse {

    @XmlElement(name = "ConfirmationDetails")
    private ConfirmationDetails confirmationDetails;

    @XmlElement(name = "error")
    private String error;

    public ConfirmationDetails getConfirmationDetails() {
        return confirmationDetails;
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
    public static class ConfirmationDetails {

        @XmlElement(name = "HotelTelephoneNo")
        private String hotelTelephoneNo;
        @XmlElement(name = "HotelStaffName")
        private String hotelStaffName;
        @XmlElement(name = "HotelConfirmationNo")
        private String hotelConfirmationNo;
        @XmlElement(name = "ConfirmationStatus")
        private String confirmationStatus;
        @XmlElement(name = "ConfirmationNote")
        private String confirmationNote;

        public String getHotelTelephoneNo() {
            return hotelTelephoneNo;
        }

        public String getHotelStaffName() {
            return hotelStaffName;
        }

        public String getHotelConfirmationNo() {
            return hotelConfirmationNo;
        }

        public String getConfirmationStatus() {
            return confirmationStatus;
        }

        public String getConfirmationNote() {
            return confirmationNote;
        }
    }
}
