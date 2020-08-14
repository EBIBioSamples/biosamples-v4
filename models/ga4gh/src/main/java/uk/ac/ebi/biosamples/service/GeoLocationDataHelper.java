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
package uk.ac.ebi.biosamples.service;

import java.util.Scanner;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.model.ga4gh.Ga4ghLocation;

/**
 * GEolocationDataHelper is util class for working with ga4gh sample location
 *
 * @author Dilshat Salikhov
 */
@Component
public class GeoLocationDataHelper {

  public boolean isGeoLocationData(String type) {
    boolean isGeolocation = type.contains("geographic location");
    isGeolocation = isGeolocation || type.contains("location");
    isGeolocation = isGeolocation || type.contains("latitude");
    isGeolocation = isGeolocation || type.contains("longitude");
    isGeolocation = isGeolocation || type.contains("altitude");
    isGeolocation = isGeolocation || type.contains("precision");
    return isGeolocation;
  }

  public Ga4ghLocation convertToDecimalDegree(String location) {
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
    return new Ga4ghLocation(latitude * nsCoef, longtitude * weCoef);
  }
}
