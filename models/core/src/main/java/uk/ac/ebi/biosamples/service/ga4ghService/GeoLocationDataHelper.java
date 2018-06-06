package uk.ac.ebi.biosamples.utils.ga4gh_utils;

import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class GeoLocationDataHelper {

    public boolean isGeoLocationData(String type) {
        boolean isGeolocation = type.contains("geographic location");
        isGeolocation = isGeolocation || type.contains("latitude");
        isGeolocation = isGeolocation || type.contains("longitude");
        isGeolocation = isGeolocation || type.contains("altitude");
        isGeolocation = isGeolocation || type.contains("precision");
        return isGeolocation;
    }

    public Location convertToDecimalDegree(String location) {
        int nsCoef = 1;
        if (location.contains("S")) {
            nsCoef = -1;
        }
        int weCoef = 1;
        if (location.contains("W")) {
            weCoef = -1;
        }
        Scanner scanner = new Scanner(location);
        double latitude = scanner.nextDouble();
        while (!scanner.hasNextDouble()) {
            scanner.next();
        }
        double longtitude = scanner.nextDouble();
        return new Location(latitude * nsCoef, longtitude * weCoef);

    }
}
