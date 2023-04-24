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
package uk.ac.ebi.biosamples.service;

import java.util.Collection;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Attribute;

@Service
public class AttributeValidator {

  public void validate(final Attribute attribute, final Collection<String> errors) {
    /*
    if (attribute.getType().length() > 255) {
    	errors.add(attribute+" type too long");
    }
    if (attribute.getValue().length() > 255) {
    	errors.add(attribute+" value too long");
    }
    if (attribute.getIri() != null && attribute.getIri().length() > 255) {
    	errors.add(attribute+" iri too long");
    }
    if (attribute.getUnit() != null && attribute.getUnit().length() > 255) {
    	errors.add(attribute+" unit too long");
    }
    */
  }
}
