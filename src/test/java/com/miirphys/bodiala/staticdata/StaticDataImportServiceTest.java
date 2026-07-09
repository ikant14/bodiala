package com.miirphys.bodiala.staticdata;

import static org.assertj.core.api.Assertions.assertThat;

import com.miirphys.bodiala.provider.rezlive.staticimport.StaticDataImportService;
import com.miirphys.bodiala.staticdata.domain.Hotel;
import com.miirphys.bodiala.staticdata.repo.CityRepository;
import com.miirphys.bodiala.staticdata.repo.CountryRepository;
import com.miirphys.bodiala.staticdata.repo.HotelImageRepository;
import com.miirphys.bodiala.staticdata.repo.HotelRepository;
import com.miirphys.bodiala.staticdata.repo.PropertyAmenityRepository;
import com.miirphys.bodiala.staticdata.repo.RoomAmenityRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StaticDataImportServiceTest {

    @Autowired
    private StaticDataImportService importService;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private HotelImageRepository hotelImageRepository;
    @Autowired
    private PropertyAmenityRepository propertyAmenityRepository;
    @Autowired
    private RoomAmenityRepository roomAmenityRepository;

    @TempDir
    Path dir;

    @BeforeEach
    void writeSampleMasterFiles() throws IOException {
        write("country.csv", """
                Name,CountryCode
                United Arab Emirates,AE
                Belgium,BE
                """);
        // City header intentionally reordered to exercise header-name mapping.
        write("city.csv", """
                CountryCode,City,Name
                AE,968,Dubai
                BE,1000,Brussels
                """);
        write("hotel-details.csv", """
                HotelCode,Name,City,CountryCode,Rating,HotelAddress,HotelPostelCode,Latitude,Longitude,Desc
                150884,Dusit Thani Dubai,968,AE,5,133 Sheikh Zayed Rd,00000,25.2285,55.2867,Nice hotel
                151641,Shangri-La Dubai,968,AE,5,Sheikh Zayed Rd,11111,25.2100,55.2900,Another hotel
                """);
        write("hotels-images.csv", """
                HotelCode,Image
                150884,http://img/a.jpg
                150884,http://img/b.jpg
                151641,http://img/c.jpg
                """);
        write("property-amenities.csv", """
                HotelCode,HotelAmenities
                150884,"Pool,Spa,WiFi"
                """);
        write("room-amenities.csv", """
                HotelCode,RoomAmenities
                150884,"TV,Minibar"
                """);
    }

    private void write(String name, String content) throws IOException {
        Files.writeString(dir.resolve(name), content);
    }

    @Test
    void importsAllMasterFilesAndJoinsOnHotelCode() {
        ImportResult result = importService.importAll(dir);

        assertThat(result.skipped()).isEmpty();
        assertThat(result.imported())
                .containsEntry("COUNTRY", 2)
                .containsEntry("CITY", 2)
                .containsEntry("HOTEL_DETAILS", 2)
                .containsEntry("HOTEL_IMAGES", 3)
                .containsEntry("PROPERTY_AMENITIES", 1)
                .containsEntry("ROOM_AMENITIES", 1);

        assertThat(cityRepository.findByCountryCode("AE"))
                .singleElement()
                .satisfies(c -> {
                    assertThat(c.getCityCode()).isEqualTo("968");
                    assertThat(c.getName()).isEqualTo("Dubai");
                });

        Hotel hotel = hotelRepository.findById(150884L).orElseThrow();
        assertThat(hotel.getName()).isEqualTo("Dusit Thani Dubai");
        assertThat(hotel.getRating()).isEqualTo(5);
        assertThat(hotel.getCityCode()).isEqualTo("968");
        assertThat(hotel.getHotelPostalCode()).isEqualTo("00000");
        assertThat(hotel.getLatitude()).isEqualTo(25.2285);

        assertThat(hotelImageRepository.findByHotelCode(150884L)).hasSize(2);
        assertThat(propertyAmenityRepository.findById(150884L).orElseThrow().getHotelAmenities())
                .contains("Pool");
        assertThat(roomAmenityRepository.findById(150884L).orElseThrow().getRoomAmenities())
                .contains("TV");
    }

    @Test
    void reimportIsIdempotent() {
        importService.importAll(dir);
        importService.importAll(dir);

        assertThat(countryRepository.count()).isEqualTo(2);
        assertThat(hotelRepository.count()).isEqualTo(2);
        assertThat(hotelImageRepository.count()).isEqualTo(3);
    }
}
