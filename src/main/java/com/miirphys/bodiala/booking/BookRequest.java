package com.miirphys.bodiala.booking;

import java.time.LocalDate;
import java.util.List;

/**
 * Our clean booking request. Use the refreshed {@code BookingKey} from the prebook response.
 * {@code agentRefNo} is your own tracking reference (the service generates one if omitted).
 */
public record BookRequest(
        String searchSessionId,
        String agentRefNo,
        LocalDate arrivalDate,
        LocalDate departureDate,
        String guestNationality,
        String countryCode,
        String city,
        String hotelId,
        String hotelName,
        String currency,
        List<BookRoom> rooms) {
}
