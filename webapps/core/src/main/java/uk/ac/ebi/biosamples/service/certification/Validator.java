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

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.validation.ValidatorI;

import java.io.IOException;
import java.io.InputStream;

@Service
@Qualifier("javaValidator")
public class Validator implements ValidatorI {
    private Logger log = LoggerFactory.getLogger(getClass());

    public void validate(String schemaPath, String document) throws IOException, ValidationException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(schemaPath)) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(rawSchema);
            schema.validate(new JSONObject(document));
        }
    }
}
