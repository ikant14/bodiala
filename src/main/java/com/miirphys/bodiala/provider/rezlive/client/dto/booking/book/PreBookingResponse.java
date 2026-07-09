package com.miirphys.bodiala.provider.rezlive.client.dto.booking.book;

import com.miirphys.bodiala.provider.rezlive.client.dto.booking.cancellation.CancellationInformations;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Response for {@code prebook}. Echoes the request (with a refreshed {@code BookingKey} per room
 * and the cancellation policy), and adds {@code <PreBookingDetails>} with the price delta.
 */
@XmlRootElement(name = "PreBookingResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class PreBookingResponse {

    @XmlElement(name = "PreBookingRequest")
    private RequestEcho preBookingRequest;

    @XmlElement(name = "PreBookingDetails")
    private PreBookingDetails preBookingDetails;

    @XmlElement(name = "error")
    private String error;

    public RequestEcho getPreBookingRequest() {
        return preBookingRequest;
    }

    public PreBookingDetails getPreBookingDetails() {
        return preBookingDetails;
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

    /** The refreshed per-room BookingKeys to carry into the booking request. */
    public List<String> refreshedBookingKeys() {
        List<String> keys = new ArrayList<>();
        if (preBookingRequest != null && preBookingRequest.preBooking != null) {
            for (EchoRoom room : preBookingRequest.preBooking.roomDetails) {
                if (room.bookingKey != null) {
                    keys.add(room.bookingKey);
                }
            }
        }
        return keys;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RequestEcho {

        @XmlElement(name = "PreBooking")
        private PreBookingEcho preBooking;

        public PreBookingEcho getPreBooking() {
            return preBooking;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PreBookingEcho {

        @XmlElementWrapper(name = "RoomDetails")
        @XmlElement(name = "RoomDetail")
        private List<EchoRoom> roomDetails = new ArrayList<>();

        @XmlElement(name = "CancellationInformations")
        private CancellationInformations cancellationInformations;

        public List<EchoRoom> getRoomDetails() {
            return roomDetails;
        }

        public CancellationInformations getCancellationInformations() {
            return cancellationInformations;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class EchoRoom {

        @XmlElement(name = "Type")
        private String type;
        @XmlElement(name = "BookingKey")
        private String bookingKey;
        @XmlElement(name = "TotalRate")
        private String totalRate;

        public String getType() {
            return type;
        }

        public String getBookingKey() {
            return bookingKey;
        }

        public String getTotalRate() {
            return totalRate;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PreBookingDetails {

        @XmlElement(name = "BookingBeforePrice")
        private String bookingBeforePrice;
        @XmlElement(name = "BookingAfterPrice")
        private String bookingAfterPrice;
        @XmlElement(name = "Difference")
        private String difference;
        @XmlElement(name = "AgentBalance")
        private String agentBalance;
        @XmlElement(name = "AgentCurrency")
        private String agentCurrency;

        public String getBookingBeforePrice() {
            return bookingBeforePrice;
        }

        public String getBookingAfterPrice() {
            return bookingAfterPrice;
        }

        public String getDifference() {
            return difference;
        }

        public String getAgentBalance() {
            return agentBalance;
        }

        public String getAgentCurrency() {
            return agentCurrency;
        }
    }
}
