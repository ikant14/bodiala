package com.miirphys.bodiala.booking;

/** A guest/occupant. {@code age} is required when {@code isChild} is true. */
public record GuestModel(
        String salutation,
        String firstName,
        String lastName,
        boolean isChild,
        Integer age) {
}
