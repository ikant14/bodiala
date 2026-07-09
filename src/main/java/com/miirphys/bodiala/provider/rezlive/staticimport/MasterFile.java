package com.miirphys.bodiala.provider.rezlive.staticimport;

import java.util.List;

/**
 * The six RezLive static-data master files, in dependency order (country -> city ->
 * hotel -> images/amenities). Each entry carries a filename keyword used to locate the
 * downloaded file in the static-data directory, and the documented canonical column names
 * (used as a fallback when the CSV has no header row).
 *
 * <p>Column names mirror the RezLive docs verbatim, including the misspelled
 * {@code HotelPostelCode} in {@link #HOTEL_DETAILS}.
 */
public enum MasterFile {

    // Keywords are truncated to tolerate singular/plural filenames (country/countries,
    // city/cities) since y-ending nouns pluralize y->ies and the real download filenames
    // are not documented by RezLive.
    COUNTRY("countr", List.of("Name", "CountryCode")),

    CITY("cit", List.of("City", "Name", "CountryCode")),

    HOTEL_DETAILS("detail", List.of(
            "HotelCode", "Name", "City", "CountryCode", "Rating",
            "HotelAddress", "HotelPostelCode", "Latitude", "Longitude", "Desc")),

    HOTEL_IMAGES("image", List.of("HotelCode", "Image")),

    PROPERTY_AMENITIES("propert", List.of("HotelCode", "HotelAmenities")),

    ROOM_AMENITIES("room", List.of("HotelCode", "RoomAmenities"));

    private final String filenameKeyword;
    private final List<String> canonicalColumns;

    MasterFile(String filenameKeyword, List<String> canonicalColumns) {
        this.filenameKeyword = filenameKeyword;
        this.canonicalColumns = canonicalColumns;
    }

    /** Lower-case substring expected in the downloaded file's name. */
    public String filenameKeyword() {
        return filenameKeyword;
    }

    public List<String> canonicalColumns() {
        return canonicalColumns;
    }
}
