
package com.nhsservicelocator.service;

import com.nhsservicelocator.data.PostcodeDataLoader;
import com.nhsservicelocator.model.GeoPoint;
import com.nhsservicelocator.model.HealthcareService;
import com.nhsservicelocator.model.SearchResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class HealthcareSearchService {

    private final List<HealthcareService> services;
    private final Map<String, GeoPoint> postcodeCoordinates;

    public HealthcareSearchService(
            List<HealthcareService> services,
            Map<String, GeoPoint> postcodeCoordinates) {

        this.services = List.copyOf(services);
        this.postcodeCoordinates = postcodeCoordinates;
    }

    public List<SearchResult> search(
            String originPostcode,
            String serviceType,
            String searchTerm,
            double maximumDistanceKm,
            int maximumResults) {

        String normalizedPostcode =
                PostcodeDataLoader.normalizePostcode(originPostcode);

        GeoPoint origin = postcodeCoordinates.get(normalizedPostcode);

        if (origin == null) {
            throw new IllegalArgumentException(
                    "Postcode not found: " + originPostcode);
        }

        String normalizedSearch = searchTerm.trim()
                .toLowerCase(Locale.ROOT);

        List<SearchResult> results = new ArrayList<>();

        for (HealthcareService service : services) {
            if (!matchesType(service, serviceType)) {
                continue;
            }

            if (!matchesText(service, normalizedSearch)) {
                continue;
            }

            GeoPoint destination = postcodeCoordinates.get(
                    PostcodeDataLoader.normalizePostcode(
                            service.postcode()));

            if (destination == null) {
                continue;
            }

            double distance = LocationService.calculateDistance(
                    origin, destination);

            if (distance <= maximumDistanceKm) {
                results.add(new SearchResult(service, distance));
            }
        }

        results.sort(
                Comparator.comparingDouble(SearchResult::distanceKm)
                        .thenComparing(
                                result -> result.service().name(),
                                String.CASE_INSENSITIVE_ORDER)
        );

        return results.subList(
                0, Math.min(maximumResults, results.size()));
    }

    public List<String> getServiceTypes() {
        Set<String> types = new TreeSet<>(
                String.CASE_INSENSITIVE_ORDER);

        for (HealthcareService service : services) {
            for (String type : service.serviceType().split("\\|")) {
                String value = type.trim();

                if (!value.isBlank()) {
                    types.add(value);
                }
            }
        }

        List<String> result = new ArrayList<>();
        result.add("All services");
        result.addAll(types);

        return result;
    }

    public int getServiceCount() {
        return services.size();
    }

    public int getPostcodeCount() {
        return postcodeCoordinates.size();
    }

    private boolean matchesType(
            HealthcareService service,
            String selectedType) {

        if (selectedType.equals("All services")) {
            return true;
        }

        for (String type : service.serviceType().split("\\|")) {
            if (type.trim().equalsIgnoreCase(selectedType)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesText(
            HealthcareService service,
            String query) {

        if (query.isBlank()) {
            return true;
        }

        return service.name().toLowerCase(Locale.ROOT).contains(query)
                || service.address().toLowerCase(Locale.ROOT).contains(query)
                || service.postcode().toLowerCase(Locale.ROOT).contains(query)
                || service.provider().toLowerCase(Locale.ROOT).contains(query);
    }
}
