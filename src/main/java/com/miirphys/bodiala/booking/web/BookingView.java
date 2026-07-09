package com.miirphys.bodiala.booking.web;

import com.miirphys.bodiala.booking.HotelBooking;
import java.time.Instant;
import java.time.LocalDate;

/** JSON view of a persisted booking (decoupled from the JPA entity). */
public record BookingView(
        Long id,
        String bookingId,
        String bookingCode,
        String agentRefNo,
        String status,
        String provider,
        String hotelId,
        String hotelName,
        String city,
        String countryCode,
        LocalDate arrivalDate,
        LocalDate departureDate,
        String currency,
        String price,
        Instant createdAt,
        Instant cancelledAt,
        String cancellationCharges) {

    public static BookingView of(HotelBooking b) {
        return new BookingView(
                b.getId(), b.getBookingId(), b.getBookingCode(), b.getAgentRefNo(), b.getStatus(),
                b.getProvider(),
                b.getHotelId(), b.getHotelName(), b.getCity(), b.getCountryCode(),
                b.getArrivalDate(), b.getDepartureDate(), b.getCurrency(), b.getPrice(),
                b.getCreatedAt(), b.getCancelledAt(), b.getCancellationCharges());
    }
}
