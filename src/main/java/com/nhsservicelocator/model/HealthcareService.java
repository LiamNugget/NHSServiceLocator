package com.nhsservicelocator.model;

public record HealthcareService(
        String id,
        String name,
        String serviceType,
        String address,
        String postcode,
        String phone,
        String website,
        String provider,
        String cqcUrl
) {
}
