package com.miirphys.bodiala.provider.hotelbeds;

import static org.assertj.core.api.Assertions.assertThat;

import com.miirphys.bodiala.provider.hotelbeds.dto.CheckRateResponse;
import com.miirphys.bodiala.provider.model.RateCheckResult;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Fixture-tests the Hotelbeds checkRates → neutral RateCheckResult mapping (HTTP-free). */
class HotelbedsBookingMapperTest {

    @Test
    void mapsCheckRatesIntoRateCheckResult() {
        CheckRateResponse.CancellationPolicy policy =
                new CheckRateResponse.CancellationPolicy("68.81", "2026-08-13T23:59:00+02:00");
        CheckRateResponse.Rate rate = new CheckRateResponse.Rate(
                "RK-FRESH", "BOOKABLE", "137.62", "162.00", "ALL INCLUSIVE", List.of(policy));
        CheckRateResponse.Room room =
                new CheckRateResponse.Room("DBT.ST", "Double or Twin STANDARD", List.of(rate));
        CheckRateResponse response = new CheckRateResponse(new CheckRateResponse.Hotel(
                "2026-08-15", "2026-08-16", 311, "Occidental Playa de Palma", "EUR", "137.62", List.of(room)));

        RateCheckResult result = HotelbedsBookingMapper.toRateCheckResult(response);

        assertThat(result.error()).isNull();

        RateCheckResult.PreBooking pb = result.preBookingRequest().preBooking();
        assertThat(pb.roomDetails()).hasSize(1);
        RateCheckResult.Room offer = pb.roomDetails().get(0);
        assertThat(offer.bookingKey()).isEqualTo("RK-FRESH#BOOKABLE");   // refreshed key + rateType
        assertThat(offer.totalRate()).isEqualTo("162.00");              // sellingRate preferred
        assertThat(offer.type()).isEqualTo("Double or Twin STANDARD");

        assertThat(pb.cancellationInformations().informations()).hasSize(1);
        RateCheckResult.Information info = pb.cancellationInformations().informations().get(0);
        assertThat(info.chargeAmount()).isEqualTo("68.81");
        assertThat(info.startDate()).isEqualTo("2026-08-13T23:59:00+02:00");
        assertThat(info.currency()).isEqualTo("EUR");

        assertThat(result.preBookingDetails().bookingAfterPrice()).isEqualTo("137.62");
        assertThat(result.preBookingDetails().agentCurrency()).isEqualTo("EUR");
    }
}
