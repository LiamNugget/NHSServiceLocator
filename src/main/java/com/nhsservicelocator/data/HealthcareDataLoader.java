
package com.nhsservicelocator.data;

import com.nhsservicelocator.model.HealthcareService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HealthcareDataLoader {

    public List<HealthcareService> load(Path filePath) throws IOException {
        List<HealthcareService> services = new ArrayList<>();

        try (CSVParser parser = CSVParser.parse(
                filePath,
                StandardCharsets.UTF_8,
                CSVFormat.DEFAULT)) {

            Map<String, Integer> columns = null;

            for (CSVRecord record : parser) {
                if (columns == null) {
                    if (record.size() > 0
                            && normalize(record.get(0)).equals("name")) {
                        columns = readColumns(record);
                    }
                    continue;
                }

                String name = getValue(record, columns, "Name");

                if (name.isBlank()) {
                    continue;
                }

                services.add(new HealthcareService(
                        getValue(record, columns, "CQC Location ID (for office use only)"),
                        name,
                        getValue(record, columns, "Service types"),
                        getValue(record, columns, "Address"),
                        getValue(record, columns, "Postcode"),
                        getValue(record, columns, "Phone number"),
                        getValue(record, columns, "Service's website (if available)"),
                        getValue(record, columns, "Provider name"),
                        getValue(record, columns, "Location URL")
                ));
            }
        }

        if (services.isEmpty()) {
            throw new IOException("No healthcare services found in " + filePath);
        }

        return List.copyOf(services);
    }

    private Map<String, Integer> readColumns(CSVRecord header) {
        Map<String, Integer> columns = new HashMap<>();

        for (int index = 0; index < header.size(); index++) {
            columns.put(normalize(header.get(index)), index);
        }

        return columns;
    }

    private String getValue(
            CSVRecord record,
            Map<String, Integer> columns,
            String columnName) {

        Integer index = columns.get(normalize(columnName));

        if (index == null || index >= record.size()) {
            return "";
        }

        return record.get(index).trim();
    }

    private String normalize(String value) {
        return value.replace("\uFEFF", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
