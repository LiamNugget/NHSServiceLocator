
package com.nhsservicelocator.data;

import com.nhsservicelocator.model.GeoPoint;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PostcodeDataLoader {

    public Map<String, GeoPoint> load(Path filePath) throws IOException {
        Map<String, GeoPoint> coordinates = new HashMap<>();

        try (Reader reader = Files.newBufferedReader(
                filePath, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.parse(reader)) {

            Map<String, Integer> columns = null;

            for (CSVRecord record : parser) {
                if (columns == null) {
                    Map<String, Integer> candidate = readColumns(record);

                    if (findColumn(candidate,
                            "postcode", "full_postcode", "pcds", "pcd") != null
                            && findColumn(candidate, "latitude", "lat") != null
                            && findColumn(candidate, "longitude", "lon", "lng", "long") != null) {
                        columns = candidate;
                    }

                    continue;
                }

                Integer postcodeIndex = findColumn(
                        columns, "postcode", "full_postcode", "pcds", "pcd");
                Integer latitudeIndex = findColumn(
                        columns, "latitude", "lat");
                Integer longitudeIndex = findColumn(
                        columns, "longitude", "lon", "lng", "long");

                if (postcodeIndex >= record.size()
                        || latitudeIndex >= record.size()
                        || longitudeIndex >= record.size()) {
                    continue;
                }

                String postcode = normalizePostcode(record.get(postcodeIndex));

                if (postcode.isBlank()) {
                    continue;
                }

                try {
                    double latitude = Double.parseDouble(
                            record.get(latitudeIndex).trim());
                    double longitude = Double.parseDouble(
                            record.get(longitudeIndex).trim());

                    coordinates.put(postcode, new GeoPoint(latitude, longitude));
                } catch (IllegalArgumentException exception) {
                    continue;
                }
            }
        }

        if (coordinates.isEmpty()) {
            throw new IOException("No valid postcode coordinates found in " + filePath);
        }

        return coordinates;
    }

    private Map<String, Integer> readColumns(CSVRecord record) {
        Map<String, Integer> columns = new HashMap<>();

        for (int index = 0; index < record.size(); index++) {
            columns.put(
                    record.get(index)
                            .replace("\uFEFF", "")
                            .trim()
                            .toLowerCase(Locale.ROOT),
                    index
            );
        }

        return columns;
    }

    private Integer findColumn(
            Map<String, Integer> columns,
            String... names) {

        for (String name : names) {
            if (columns.containsKey(name)) {
                return columns.get(name);
            }
        }

        return null;
    }

    public static String normalizePostcode(String postcode) {
        return postcode.replaceAll("\\s+", "")
                .toUpperCase(Locale.UK)
                .trim();
    }
}
