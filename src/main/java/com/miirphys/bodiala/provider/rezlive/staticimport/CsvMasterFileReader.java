package com.miirphys.bodiala.provider.rezlive.staticimport;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads a RezLive CSV master file into a list of {@link Row}s keyed by canonical column name.
 *
 * <p>The exact CSV layout (header row? column order? delimiter?) is NOT documented by RezLive
 * — the real files sit behind a credentialed panel. This reader is therefore defensive:
 * <ul>
 *   <li>If the first record looks like a header (its cells match the documented column
 *       names), columns are mapped by name — robust to any column ordering.</li>
 *   <li>Otherwise columns are mapped positionally from the documented canonical order.</li>
 * </ul>
 * Values are exposed by canonical name (case-insensitive) with lenient numeric parsing.
 */
public final class CsvMasterFileReader {

    private static final Logger log = LoggerFactory.getLogger(CsvMasterFileReader.class);

    private CsvMasterFileReader() {
    }

    /** A single data row, values addressable by canonical column name (case-insensitive). */
    public static final class Row {

        private final Map<String, String> byName;

        private Row(Map<String, String> byName) {
            this.byName = byName;
        }

        public String getString(String column) {
            String v = byName.get(column.toLowerCase(Locale.ROOT));
            if (v == null) {
                return null;
            }
            v = v.trim();
            return v.isEmpty() ? null : v;
        }

        public Long getLong(String column) {
            String v = getString(column);
            if (v == null) {
                return null;
            }
            try {
                return Long.parseLong(stripDecimalZeros(v));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        public Integer getInt(String column) {
            Long v = getLong(column);
            return v == null ? null : v.intValue();
        }

        public Double getDouble(String column) {
            String v = getString(column);
            if (v == null) {
                return null;
            }
            try {
                return Double.parseDouble(v);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private static String stripDecimalZeros(String v) {
            int dot = v.indexOf('.');
            return dot < 0 ? v : v.substring(0, dot);
        }
    }

    /**
     * Parse the given reader for the given master file. The reader is fully consumed and closed.
     */
    public static List<Row> read(Reader reader, MasterFile masterFile) throws IOException {
        List<String> canonical = masterFile.canonicalColumns();
        List<Row> rows = new ArrayList<>();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setIgnoreEmptyLines(true)
                .setIgnoreSurroundingSpaces(true)
                .build();

        try (CSVParser parser = CSVParser.parse(reader, format)) {
            Map<String, Integer> columnIndex = null; // canonical(lower) -> record position
            boolean positional = false;
            int aritySkipped = 0;

            for (CSVRecord record : parser) {
                if (columnIndex == null) {
                    // Decide whether this first record is a header row.
                    if (looksLikeHeader(record, canonical)) {
                        columnIndex = indexFromHeader(record, canonical);
                        warnUnmatchedColumns(masterFile, canonical, columnIndex);
                        continue; // header consumed; do not emit as data
                    }
                    columnIndex = indexPositional(canonical);
                    positional = true;
                    // fall through: this record is data
                }
                // In positional (no-header) mode we cannot trust a record whose column count
                // differs from the documented schema — the fields would be silently shifted.
                // Skip such rows rather than corrupt the table.
                if (positional && record.size() != canonical.size()) {
                    aritySkipped++;
                    continue;
                }
                rows.add(toRow(record, columnIndex));
            }

            if (aritySkipped > 0) {
                log.warn("{}: skipped {} row(s) whose column count != {} in positional (no-header) "
                                + "mode — verify the CSV layout against a real RezLive download",
                        masterFile, aritySkipped, canonical.size());
            }
        }
        return rows;
    }

    /**
     * Treat the first record as a header only on a strong signal — at least half of the
     * documented columns present as exact cell matches — so an occasional data value that
     * happens to equal a column name does not cause a real row to be dropped.
     */
    private static boolean looksLikeHeader(CSVRecord record, List<String> canonical) {
        Set<String> canon = lowerSet(canonical);
        int matches = 0;
        for (int i = 0; i < record.size(); i++) {
            String cell = record.get(i);
            if (cell != null && canon.contains(cell.trim().toLowerCase(Locale.ROOT))) {
                matches++;
            }
        }
        int required = Math.max(1, (int) Math.ceil(canonical.size() / 2.0));
        return matches >= required;
    }

    private static void warnUnmatchedColumns(MasterFile masterFile, List<String> canonical,
                                             Map<String, Integer> columnIndex) {
        List<String> missing = canonical.stream()
                .filter(c -> !columnIndex.containsKey(c.toLowerCase(Locale.ROOT)))
                .toList();
        if (!missing.isEmpty()) {
            log.warn("{}: header detected but documented column(s) {} were not found — they will be "
                            + "null for every row; check for a spelling difference in the real file",
                    masterFile, missing);
        }
    }

    private static Map<String, Integer> indexFromHeader(CSVRecord header, List<String> canonical) {
        Map<String, Integer> headerPos = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String name = header.get(i);
            if (name != null) {
                headerPos.put(name.trim().toLowerCase(Locale.ROOT), i);
            }
        }
        Map<String, Integer> index = new HashMap<>();
        for (String col : canonical) {
            String key = col.toLowerCase(Locale.ROOT);
            Integer pos = headerPos.get(key);
            if (pos != null) {
                index.put(key, pos);
            }
        }
        return index;
    }

    private static Map<String, Integer> indexPositional(List<String> canonical) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < canonical.size(); i++) {
            index.put(canonical.get(i).toLowerCase(Locale.ROOT), i);
        }
        return index;
    }

    private static Row toRow(CSVRecord record, Map<String, Integer> columnIndex) {
        Map<String, String> byName = new HashMap<>();
        for (Map.Entry<String, Integer> e : columnIndex.entrySet()) {
            int pos = e.getValue();
            if (pos >= 0 && pos < record.size()) {
                byName.put(e.getKey(), record.get(pos));
            }
        }
        return new Row(byName);
    }

    private static Set<String> lowerSet(List<String> values) {
        Set<String> set = new LinkedHashSet<>();
        for (String v : values) {
            set.add(v.toLowerCase(Locale.ROOT));
        }
        return set;
    }
}
