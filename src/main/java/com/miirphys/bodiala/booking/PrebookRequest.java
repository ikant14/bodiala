package com.miirphys.bodiala.booking;

import java.time.LocalDate;
import java.util.List;

/** Our clean prebook request. Dates are ISO; converted to RezLive dd/MM/yyyy by the service. */
public record PrebookRequest(
        String searchSessionId,
        LocalDate arrivalDate,
        LocalDate departureDate,
        String guestNationality,
        String countryCode,
        String city,
        String hotelId,
        String currency,
        List<RoomOffer> rooms) {
}
