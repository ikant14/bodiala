package com.miirphys.bodiala.staticdata.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Room Amenities master record. RezLive CSV columns: {@code HotelCode, RoomAmenities}.
 * Documented as one row per hotel with a list of amenities, so {@code hotelCode} is the key.
 */
@Entity
@Table(name = "room_amenity")
public class RoomAmenity {

    @Id
    @Column(name = "hotel_code", nullable = false)
    private Long hotelCode;

    @Lob
    @Column(name = "room_amenities")
    private String roomAmenities;

    protected RoomAmenity() {
    }

    public RoomAmenity(Long hotelCode, String roomAmenities) {
        this.hotelCode = hotelCode;
        this.roomAmenities = roomAmenities;
    }

    public Long getHotelCode() {
        return hotelCode;
    }

    public void setHotelCode(Long hotelCode) {
        this.hotelCode = hotelCode;
    }

    public String getRoomAmenities() {
        return roomAmenities;
    }

    public void setRoomAmenities(String roomAmenities) {
        this.roomAmenities = roomAmenities;
    }
}
