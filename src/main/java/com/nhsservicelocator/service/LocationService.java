
package com.nhsservicelocator.service;

import com.nhsservicelocator.model.GeoPoint;

public final class LocationService {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private LocationService() {
    }

    public static double calculateDistance(
            GeoPoint origin,
            GeoPoint destination) {

        double latitudeDifference = Math.toRadians(
                destination.latitude() - origin.latitude());

        double longitudeDifference = Math.toRadians(
                destination.longitude() - origin.longitude());

        double firstLatitude = Math.toRadians(origin.latitude());
        double secondLatitude = Math.toRadians(destination.latitude());

        double haversine = Math.pow(
                Math.sin(latitudeDifference / 2), 2)
                + Math.cos(firstLatitude)
                * Math.cos(secondLatitude)
                * Math.pow(Math.sin(longitudeDifference / 2), 2);

        return 2 * EARTH_RADIUS_KM
                * Math.asin(Math.min(1.0, Math.sqrt(haversine)));
    }
}
