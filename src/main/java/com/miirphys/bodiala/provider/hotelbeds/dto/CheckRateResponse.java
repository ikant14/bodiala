package com.miirphys.bodiala.provider.hotelbeds.dto;

import java.util.List;

/**
 * Subset of the Hotelbeds {@code POST /hotel-api/1.0/checkrates} response. NOTE: the live JSON uses
 * a singular {@code hotel} wrapper (the OpenAPI schema says {@code hotels}) — confirm against the
 * sandbox (see api-contract.md §2). Carries the refreshed {@code rateKey} to book with.
 */
public record CheckRateResponse(Hotel hotel) {

    public record Hotel(
            String checkIn, String checkOut, Integer code, String name,
            String currency, String totalNet, List<Room> rooms) {
    }

    public record Room(String code, String name, List<Rate> rates) {
    }

    public record Rate(
            String rateKey, String rateType, String net, String sellingRate,
            String boardName, List<CancellationPolicy> cancellationPolicies) {
    }

    public record CancellationPolicy(String amount, String from) {
    }
}
