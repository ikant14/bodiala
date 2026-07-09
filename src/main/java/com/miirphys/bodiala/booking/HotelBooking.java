package com.miirphys.bodiala.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A booking we made through RezLive, persisted locally. Keyed by our surrogate id; the RezLive
 * {@code BookingId}/{@code BookingCode} pair (needed for cancel / details) is stored alongside.
 */
@Entity
@Table(name = "hotel_booking", indexes = {
        @Index(name = "idx_booking_rez_id", columnList = "rez_booking_id", unique = true),
        @Index(name = "idx_booking_agent_ref", columnList = "agent_ref_no", unique = true)
})
public class HotelBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rez_booking_id", nullable = false, unique = true)
    private String bookingId;

    @Column(name = "rez_booking_code")
    private String bookingCode;

    @Column(name = "agent_ref_no")
    private String agentRefNo;

    @Column(name = "status")
    private String status;

    /**
     * Which supplier created this booking (see {@code com.miirphys.bodiala.provider.ProviderId}).
     * New rows are stamped by their provider; rows written before the multi-provider split — and any
     * left NULL by the additive migration — read as {@code REZLIVE}.
     */
    @Column(name = "provider")
    private String provider = "REZLIVE";

    @Column(name = "hotel_id")
    private String hotelId;

    @Column(name = "hotel_name")
    private String hotelName;

    @Column(name = "city")
    private String city;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(name = "currency")
    private String currency;

    @Column(name = "price")
    private String price;

    @Column(name = "search_session_id", length = 1024)
    private String searchSessionId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_charges")
    private String cancellationCharges;

    public Long getId() {
        return id;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public String getAgentRefNo() {
        return agentRefNo;
    }

    public void setAgentRefNo(String agentRefNo) {
        this.agentRefNo = agentRefNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getHotelId() {
        return hotelId;
    }

    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
    }

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public LocalDate getArrivalDate() {
        return arrivalDate;
    }

    public void setArrivalDate(LocalDate arrivalDate) {
        this.arrivalDate = arrivalDate;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getSearchSessionId() {
        return searchSessionId;
    }

    public void setSearchSessionId(String searchSessionId) {
        this.searchSessionId = searchSessionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellationCharges() {
        return cancellationCharges;
    }

    public void setCancellationCharges(String cancellationCharges) {
        this.cancellationCharges = cancellationCharges;
    }
}
