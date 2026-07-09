package com.miirphys.bodiala.provider.hotelbeds;

import com.miirphys.bodiala.provider.hotelbeds.dto.CheckRateResponse;
import com.miirphys.bodiala.provider.model.RateCheckResult;
import java.util.List;
import java.util.stream.Stream;

/**
 * Maps a Hotelbeds {@code checkRates} response into the neutral {@link RateCheckResult}. Pure /
 * HTTP-free (fixture-testable). The refreshed per-room {@code rateKey} (carrying {@code #rateType})
 * becomes the offer key to book with; cancellation policies become the informations block.
 */
public final class HotelbedsBookingMapper {

    private HotelbedsBookingMapper() {
    }

    public static RateCheckResult toRateCheckResult(CheckRateResponse response) {
        CheckRateResponse.Hotel hotel = response == null ? null : response.hotel();
        List<CheckRateResponse.Room> rooms = (hotel == null || hotel.rooms() == null) ? List.of() : hotel.rooms();
        String currency = hotel == null ? null : hotel.currency();

        List<RateCheckResult.Room> offerRooms = rooms.stream()
                .flatMap(room -> room.rates() == null ? Stream.empty()
                        : room.rates().stream().map(rate -> toOfferRoom(room, rate)))
                .toList();

        RateCheckResult.CancellationInformations cancellation = firstCancellation(rooms, currency);
        RateCheckResult.RequestEcho echo =
                new RateCheckResult.RequestEcho(new RateCheckResult.PreBooking(offerRooms, cancellation));

        String total = hotel == null ? null : hotel.totalNet();
        RateCheckResult.PriceDetails details =
                new RateCheckResult.PriceDetails(null, total, null, null, currency);

        return new RateCheckResult(echo, details, null);
    }

    private static RateCheckResult.Room toOfferRoom(CheckRateResponse.Room room, CheckRateResponse.Rate rate) {
        String bookingKey = rate.rateType() == null ? rate.rateKey() : rate.rateKey() + "#" + rate.rateType();
        String totalRate = (rate.sellingRate() != null && !rate.sellingRate().isBlank())
                ? rate.sellingRate() : rate.net();
        return new RateCheckResult.Room(room.name(), bookingKey, totalRate);
    }

    private static RateCheckResult.CancellationInformations firstCancellation(
            List<CheckRateResponse.Room> rooms, String currency) {
        List<CheckRateResponse.CancellationPolicy> policies = rooms.stream()
                .filter(r -> r.rates() != null)
                .flatMap(r -> r.rates().stream())
                .filter(rate -> rate.cancellationPolicies() != null && !rate.cancellationPolicies().isEmpty())
                .findFirst()
                .map(CheckRateResponse.Rate::cancellationPolicies)
                .orElse(List.of());
        if (policies.isEmpty()) {
            return null;
        }
        List<RateCheckResult.Information> infos = policies.stream()
                .map(p -> new RateCheckResult.Information(p.from(), null, "Amount", p.amount(), currency))
                .toList();
        return new RateCheckResult.CancellationInformations(infos, null);
    }
}
