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
package uk.ac.ebi.biosamples.service.certification;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;
import uk.ac.ebi.biosamples.model.certification.Checklist;
import uk.ac.ebi.biosamples.service.validation.ValidatorI;

@Service
@Qualifier("javaValidator")
public class Validator implements ValidatorI {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final ConfigLoader configLoader;
  private Map<String, Checklist> checklists;

  public Validator(final ConfigLoader configLoader) {
    this.configLoader = configLoader;
  }

  private void init() {
    final List<Checklist> checklistList = configLoader.config.getChecklists();
    checklists = checklistList.stream().collect(Collectors.toMap(Checklist::getID, c -> c));

    // to validate schemas without the version
    for (final Checklist c : checklistList) {
      if (checklists.containsKey(c.getName())) {
        if (checklists.get(c.getName()).getVersion().compareTo(c.getVersion()) < 0) {
          checklists.put(c.getName(), c);
        }
      } else {
        checklists.put(c.getName(), c);
      }
    }
  }

  @Override
  public void validate(final String schemaPath, final String document)
      throws IOException, ValidationException {
    try (final InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream(schemaPath)) {
      final JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
      final Schema schema = SchemaLoader.load(rawSchema);
      schema.validate(new JSONObject(document));
    }
  }

  @Override
  public String validateById(final String schemaId, final String document)
      throws IOException, GlobalExceptions.SchemaValidationException {
    final Checklist checklist = getChecklist(schemaId);
    try (final InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream(checklist.getFileName())) {
      final JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
      final Schema schema = SchemaLoader.load(rawSchema);
      schema.validate(new JSONObject(document));
    } catch (final ValidationException e) {
      throw new GlobalExceptions.SchemaValidationException(e.getMessage());
    }

    return checklist.getID();
  }

  private Checklist getChecklist(final String checklistId) {
    if (checklists == null) {
      init();
    }
    return checklists.get(checklistId);
  }
}
