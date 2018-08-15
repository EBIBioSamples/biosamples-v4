package uk.ac.ebi.biosamples.model.ga4gh;

import java.util.Objects;

public class Ga4ghLocation {
    private double latitude;
    private double longtitude;

    public Ga4ghLocation(double latitude, double longtitude) {
        this.latitude = latitude;
        this.longtitude = longtitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongtitude() {
        return longtitude;
    }

    public void setLongtitude(double longtitude) {
        this.longtitude = longtitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ga4ghLocation)) return false;
        Ga4ghLocation location = (Ga4ghLocation) o;
        return Double.compare(location.latitude, latitude) == 0 &&
                Double.compare(location.longtitude, longtitude) == 0;
    }

    @Override
    public int hashCode() {

        return Objects.hash(latitude, longtitude);
    }
}
