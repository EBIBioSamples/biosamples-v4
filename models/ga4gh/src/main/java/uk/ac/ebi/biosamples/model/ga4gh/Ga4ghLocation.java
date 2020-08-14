/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
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
    return Double.compare(location.latitude, latitude) == 0
        && Double.compare(location.longtitude, longtitude) == 0;
  }

  @Override
  public int hashCode() {

    return Objects.hash(latitude, longtitude);
  }
}
