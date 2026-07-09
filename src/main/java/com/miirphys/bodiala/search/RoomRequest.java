package com.miirphys.bodiala.search;

import java.util.List;

/**
 * One requested room. {@code childrenAges} is required when {@code noOfChilds > 0}.
 */
public record RoomRequest(
        String type,
        Integer noOfAdults,
        Integer noOfChilds,
        List<Integer> childrenAges) {
}
