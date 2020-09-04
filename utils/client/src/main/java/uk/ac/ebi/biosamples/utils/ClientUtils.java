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
package uk.ac.ebi.biosamples.utils;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestOperations;

public class ClientUtils {

  private Logger log = LoggerFactory.getLogger(getClass());

  public static <U, V> ResponseEntity<V> doRetryQuery(
      final RequestEntity<U> requestEntity,
      final RestOperations restOperations,
      final int maxRetries,
      ParameterizedTypeReference<V> parameterizedTypeReference) {
    ResponseEntity<V> responseEntity = null;
    int retries = 0;
    while (responseEntity == null && retries < maxRetries) {
      try {
        responseEntity = restOperations.exchange(requestEntity, parameterizedTypeReference);
      } catch (HttpStatusCodeException e) {
        if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS
            || e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
          // need to wait and retry
          String retryString = "";
          // may include a header for how long to wait till retry
          if (responseEntity.getHeaders().keySet().contains(HttpHeaders.RETRY_AFTER)) {
            retryString = responseEntity.getHeaders().get(HttpHeaders.RETRY_AFTER).get(0);
          }
          // retry header after could be an integer number of seconds, or a date
          ZonedDateTime retryTime = null;
          try {
            retryTime = ZonedDateTime.parse(retryString, DateTimeFormatter.RFC_1123_DATE_TIME);
          } catch (DateTimeParseException e2) {
            // do nothing
          }
          if (retryTime == null) {
            int delaySeconds = 10 * retries; // default to waiting 10 seconds per retry before
            // trying again
            try {
              delaySeconds = Integer.parseInt(retryString);
            } catch (NumberFormatException e2) {
              // do nothing
            }
            // TODO use the date/time from the response, instead of now?
            retryTime = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(delaySeconds);
          }

          // at this point, we will have a time after which we can retry
          // sleep until after that time
          while (ZonedDateTime.now(ZoneOffset.UTC).isBefore(retryTime)) {
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e2) {
              throw new RuntimeException(e2);
            }
          }

          // now we can retry and hope it works
          retries += 1;
          responseEntity = null;

        } else {
          throw e;
        }
      }
    }
    return responseEntity;
  }
}
