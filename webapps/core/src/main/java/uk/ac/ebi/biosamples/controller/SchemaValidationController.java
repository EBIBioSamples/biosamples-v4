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
package uk.ac.ebi.biosamples.controller;

import java.util.Optional;

import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.exception.SchemaValidationException;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SchemaValidationService;

@RestController
@CrossOrigin
@RequestMapping("/validate")
public class SchemaValidationController {
  private final SchemaValidationService schemaValidationService;

  public SchemaValidationController(SchemaValidationService schemaValidationService) {
    this.schemaValidationService = schemaValidationService;
  }

  @PostMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Sample> validate(
      @RequestBody Sample sample,
      @RequestParam(name = "checklist", required = false) String checklist) {

    if (checklist != null && !checklist.isEmpty()) {
      Attribute checklistAttribute = Attribute.build("checklist", checklist);
      Optional<Attribute> optionalChecklist =
          sample.getCharacteristics().stream()
              .filter(c -> c.getType().equalsIgnoreCase("checklist"))
              .findFirst();
      if (optionalChecklist.isPresent()) {
        if (!optionalChecklist.get().getValue().equals(checklistAttribute.getValue())) {
          throw new SchemaValidationException(
              "Different checklist IDs in body and request parameter");
        }
      } else {
        sample.getCharacteristics().add(checklistAttribute);
      }
    }

    schemaValidationService.validate(sample);
    return ResponseEntity.ok(sample);
  }
}
