/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.mongo.service;

import com.mongodb.WriteConcern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoAction;
import org.springframework.data.mongodb.core.WriteConcernResolver;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.mongo.MongoProperties;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;

// TODO wire this into config
@Component
public class CustomWriteConcernResolver implements WriteConcernResolver {

  private Logger log = LoggerFactory.getLogger(getClass());

  @Autowired private MongoProperties mongoProperties;

  @Override
  public WriteConcern resolve(MongoAction action) {
    log.trace("Resolving mongoAction " + action);

    if (MongoSample.class.isAssignableFrom(action.getEntityType())) {
      final String sampleWriteConcern = mongoProperties.getSampleWriteConcern();

      if (sampleWriteConcern.matches("[0-9]+")) {
        return  new WriteConcern(Integer.parseInt(sampleWriteConcern));
      } else {
        return  new WriteConcern(sampleWriteConcern);
      }
    }

    return action.getDefaultWriteConcern();
  }
}
