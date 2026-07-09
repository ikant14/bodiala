package com.miirphys.bodiala.staticdata;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Outcome of a static-data import run: how many rows were loaded per master file, and
 * which files were skipped (not found in the static-data directory).
 */
public record ImportResult(Map<String, Integer> imported, Map<String, String> skipped) {

    public static ImportResult empty() {
        return new ImportResult(new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    public int totalRows() {
        return imported.values().stream().mapToInt(Integer::intValue).sum();
    }
}
