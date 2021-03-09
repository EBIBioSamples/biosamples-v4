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
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.certification.Checklist;

@Service
public class Validator {
  private Logger log = LoggerFactory.getLogger(getClass());

  private ConfigLoader configLoader;
  private Map<String, Checklist> checklists;

  public Validator(ConfigLoader configLoader) {
    this.configLoader = configLoader;
  }

  private void init() {
    List<Checklist> checklistList = configLoader.config.getChecklists();
    checklists = checklistList.stream().collect(Collectors.toMap(Checklist::getID, c -> c));

    // to validate schemas without the version
    for (Checklist c : checklistList) {
      if (this.checklists.containsKey(c.getName())) {
        if (this.checklists.get(c.getName()).getVersion().compareTo(c.getVersion()) < 0) {
          this.checklists.put(c.getName(), c);
        }
      } else {
        this.checklists.put(c.getName(), c);
      }
    }
  }

  public void validate(String schemaPath, String document) throws IOException, ValidationException {
    log.info("Schema path is " + schemaPath);

    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(schemaPath)) {
      JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
      Schema schema = SchemaLoader.load(rawSchema);
      schema.validate(new JSONObject(document));
    }
  }

  public String validateById(String schemaId, String document) throws IOException, ValidationException {
    Checklist checklist = getChecklist(schemaId);
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(checklist.getFileName())) {
      JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
      Schema schema = SchemaLoader.load(rawSchema);
      schema.validate(new JSONObject(document));
    }

    return checklist.getID();
  }

  private Checklist getChecklist(String checklistId) {
    if (checklists == null) {
      init();
    }
    return checklists.get(checklistId);
  }
}
