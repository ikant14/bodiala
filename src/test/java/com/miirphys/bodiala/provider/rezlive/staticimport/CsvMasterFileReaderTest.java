package com.miirphys.bodiala.provider.rezlive.staticimport;

import static org.assertj.core.api.Assertions.assertThat;

import com.miirphys.bodiala.provider.rezlive.staticimport.CsvMasterFileReader.Row;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvMasterFileReaderTest {

    @Test
    void mapsByHeaderNameRegardlessOfColumnOrder() throws IOException {
        // Header columns deliberately reordered vs the documented canonical order.
        String csv = """
                CountryCode,City,Name
                AE,968,Dubai
                BE,1000,Brussels
                """;
        List<Row> rows = CsvMasterFileReader.read(new StringReader(csv), MasterFile.CITY);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getString("City")).isEqualTo("968");
        assertThat(rows.get(0).getString("Name")).isEqualTo("Dubai");
        assertThat(rows.get(0).getString("CountryCode")).isEqualTo("AE");
    }

    @Test
    void fallsBackToPositionalOrderWhenNoHeader() throws IOException {
        // No header row — must map by documented canonical order [Name, CountryCode].
        String csv = """
                United Arab Emirates,AE
                Belgium,BE
                """;
        List<Row> rows = CsvMasterFileReader.read(new StringReader(csv), MasterFile.COUNTRY);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getString("Name")).isEqualTo("United Arab Emirates");
        assertThat(rows.get(0).getString("CountryCode")).isEqualTo("AE");
    }

    @Test
    void parsesNumericColumnsLeniently() throws IOException {
        String csv = """
                HotelCode,Name,City,CountryCode,Rating,HotelAddress,HotelPostelCode,Latitude,Longitude,Desc
                150884,Dusit Thani,968,AE,5,133 Sheikh Zayed Rd,00000,25.2285,55.2867,Nice
                """;
        List<Row> rows = CsvMasterFileReader.read(new StringReader(csv), MasterFile.HOTEL_DETAILS);

        Row row = rows.get(0);
        assertThat(row.getLong("HotelCode")).isEqualTo(150884L);
        assertThat(row.getInt("Rating")).isEqualTo(5);
        assertThat(row.getDouble("Latitude")).isEqualTo(25.2285);
        assertThat(row.getString("HotelPostelCode")).isEqualTo("00000");
        assertThat(row.getString("Desc")).isEqualTo("Nice");
    }

    @Test
    void positionalModeSkipsRowsWithWrongColumnCount() throws IOException {
        // Headerless file with a stray leading index column: arity 3 != canonical 2. Rather than
        // silently shift every field, such rows are skipped.
        String csv = """
                0,United Arab Emirates,AE
                1,Belgium,BE
                """;
        List<Row> rows = CsvMasterFileReader.read(new StringReader(csv), MasterFile.COUNTRY);

        assertThat(rows).isEmpty();
    }

    @Test
    void singleAccidentalMatchIsNotTreatedAsHeader() throws IOException {
        // A 3-column city file with no header whose first data row happens to contain one cell
        // equal to a column name ("Name") must NOT be misread as a header and dropped.
        String csv = """
                968,Name,AE
                1000,Brussels,BE
                """;
        List<Row> rows = CsvMasterFileReader.read(new StringReader(csv), MasterFile.CITY);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getString("City")).isEqualTo("968");
    }

    @Test
    void blankCellsBecomeNull() throws IOException {
        String csv = """
                HotelCode,Name,City,CountryCode,Rating,HotelAddress,HotelPostelCode,Latitude,Longitude,Desc
                150884,Dusit Thani,968,AE,,,,,,
                """;
        Row row = CsvMasterFileReader.read(new StringReader(csv), MasterFile.HOTEL_DETAILS).get(0);

        assertThat(row.getInt("Rating")).isNull();
        assertThat(row.getDouble("Latitude")).isNull();
        assertThat(row.getString("Desc")).isNull();
    }
}
