package com.miirphys.bodiala.booking;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelBookingRepository extends JpaRepository<HotelBooking, Long> {

    Optional<HotelBooking> findByBookingId(String bookingId);

    Optional<HotelBooking> findByAgentRefNo(String agentRefNo);
}
