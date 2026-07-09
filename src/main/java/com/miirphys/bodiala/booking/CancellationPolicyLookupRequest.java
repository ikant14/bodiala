package com.miirphys.bodiala.booking;

import java.time.LocalDate;
import java.util.List;

/** Our clean pre-booking cancellation-policy lookup request (maps to getcancellationpolicy). */
public record CancellationPolicyLookupRequest(
        LocalDate arrivalDate,
        LocalDate departureDate,
        String hotelId,
        String countryCode,
        String city,
        String guestNationality,
        String currency,
        List<PolicyRoom> rooms) {
}
