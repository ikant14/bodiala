package com.miirphys.bodiala.provider.model;

import java.util.List;

/**
 * Provider-neutral result of the pre-book / rate-check step (RezLive {@code prebook} ≡ Hotelbeds
 * {@code checkRates}). Its JSON mirrors what the frontend consumed from RezLive's
 * {@code PreBookingResponse}, so the API contract is unchanged: the refreshed per-room offer keys +
 * cancellation policy live under {@code preBookingRequest.preBooking}, and the price delta under
 * {@code preBookingDetails}.
 */
public record RateCheckResult(
        RequestEcho preBookingRequest,
        PriceDetails preBookingDetails,
        String error) {

    public record RequestEcho(PreBooking preBooking) {
    }

    public record PreBooking(List<Room> roomDetails, CancellationInformations cancellationInformations) {
    }

    public record Room(String type, String bookingKey, String totalRate) {
    }

    public record CancellationInformations(List<Information> informations, String info) {
    }

    public record Information(
            String startDate, String endDate, String chargeType, String chargeAmount, String currency) {
    }

    public record PriceDetails(
            String bookingBeforePrice,
            String bookingAfterPrice,
            String difference,
            String agentBalance,
            String agentCurrency) {
    }
}
