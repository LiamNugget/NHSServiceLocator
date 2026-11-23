package com.nhsservicelocator.model;

public record SearchResult(
        HealthcareService service,
        double distanceKm
) {
}
