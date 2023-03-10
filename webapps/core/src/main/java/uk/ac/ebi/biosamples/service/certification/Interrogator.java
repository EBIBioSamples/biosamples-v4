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
import java.util.ArrayList;
import java.util.List;
import org.everit.json.schema.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.certification.Checklist;
import uk.ac.ebi.biosamples.model.certification.InterrogationResult;
import uk.ac.ebi.biosamples.model.certification.SampleDocument;
import uk.ac.ebi.biosamples.validation.ValidatorI;

@Service
public class Interrogator {
  private static final Logger LOG = LoggerFactory.getLogger(Interrogator.class);
  private static final Logger EVENTS = LoggerFactory.getLogger("events");

  private final ConfigLoader configLoader;
  private final ValidatorI validator;

  public Interrogator(
      final ConfigLoader configLoader, @Qualifier("javaValidator") final ValidatorI validator) {
    this.validator = validator;
    this.configLoader = configLoader;
  }

  public InterrogationResult interrogate(final SampleDocument sampleDocument) {
    if (sampleDocument == null) {
      final String message = "cannot interrogate a null sampleDocument";
      LOG.warn(message);
      throw new IllegalArgumentException(message);
    }

    final List<Checklist> checklists = new ArrayList<>();

    for (final Checklist checklist : configLoader.config.getChecklists()) {
      try {
        validator.validate(checklist.getFileName(), sampleDocument.getDocument());
        EVENTS.info(
            String.format(
                "%s interrogation successful against %s",
                sampleDocument.getAccession(), checklist.getID()));
        checklists.add(checklist);
      } catch (final IOException ioe) {
        LOG.error(String.format("cannot open schema at %s", checklist.getFileName()), ioe);
      } catch (final ValidationException ve) {
        EVENTS.info(
            String.format(
                "%s interrogation failed against %s",
                sampleDocument.getAccession(), checklist.getID()));
      }
    }

    return new InterrogationResult(sampleDocument, checklists);
  }
}
