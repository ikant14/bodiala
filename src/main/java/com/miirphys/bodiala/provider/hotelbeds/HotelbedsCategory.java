package com.miirphys.bodiala.provider.hotelbeds;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves a Hotelbeds category code to a numeric star rating. Hotelbeds sends the category as a code
 * such as {@code "4EST"} (estrellas / stars) or {@code "3LL"} (llaves / keys) — never a plain number —
 * so both the content-catalog and availability paths extract the leading digit through here to keep the
 * cached {@code Hotel.rating} and the neutral search-result {@code rating} identical.
 */
final class HotelbedsCategory {

    private static final Pattern LEADING_DIGITS = Pattern.compile("(\\d+)");

    private HotelbedsCategory() {
    }

    /** Star count from a category code, e.g. {@code "4EST"} → {@code 4}; {@code null}/no digits → {@code null}. */
    static Integer stars(String categoryCode) {
        if (categoryCode == null) {
            return null;
        }
        Matcher m = LEADING_DIGITS.matcher(categoryCode);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }
}
