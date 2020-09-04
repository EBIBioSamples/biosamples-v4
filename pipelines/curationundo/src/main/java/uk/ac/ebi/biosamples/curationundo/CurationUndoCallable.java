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
package uk.ac.ebi.biosamples.curationundo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;

public class CurationUndoCallable implements Callable<Void> {

  private Logger log = LoggerFactory.getLogger(getClass());

  private final String accession;
  private final boolean canary;
  private final BioSamplesClient bioSamplesClient;
  private final Instant instant;

  public CurationUndoCallable(BioSamplesClient bioSamplesClient, String accession, boolean canary) {
    this.bioSamplesClient = bioSamplesClient;
    this.accession = accession;
    this.canary = canary;
    LocalDate localDate = LocalDate.parse("2018-10-23");
    LocalDateTime localDateTime = localDate.atStartOfDay();
    instant = localDateTime.toInstant(ZoneOffset.UTC);
  }

  public static final String[] NON_APPLICABLE_SYNONYMS = {
    "n/a",
    "na",
    "n.a",
    "none",
    "unknown",
    "--",
    ".",
    "null",
    "missing",
    "[not reported]",
    "[not requested]",
    "not applicable",
    "not_applicable",
    "not collected",
    "not specified",
    "not known",
    "not reported"
  };

  public static boolean wouldHaveBeenRemoved(String string) {
    String lsString = string.toLowerCase().trim();
    return Arrays.stream(NON_APPLICABLE_SYNONYMS).parallel().anyMatch(lsString::contains);
  }

  public static boolean shouldHaveBeenRemoved(String string) {
    String lsString = string.toLowerCase().trim();
    return Arrays.stream(NON_APPLICABLE_SYNONYMS).parallel().anyMatch(lsString::equals);
  }

  @Override
  public Void call() throws Exception {
    for (Resource<CurationLink> cl : bioSamplesClient.fetchCurationLinksOfSample(accession)) {
      if (cl.getContent().getDomain().equals("self.BiosampleCuration")
          && (cl.getContent().getCreated().isAfter(instant))) {
        Curation curation = cl.getContent().getCuration();
        Attribute firstPreAtt = curation.getAttributesPre().first();
        String post = "REMOVED";
        try {
          post = curation.getAttributesPost().first().getValue();
        } catch (NoSuchElementException e) {
        }
        if (wouldHaveBeenRemoved(firstPreAtt.getValue())
            && !shouldHaveBeenRemoved(firstPreAtt.getValue())) {
          if (canary) {
            String pre = firstPreAtt.getType() + " : " + firstPreAtt.getValue();
            log.info("Sample: " + accession + "\t" + pre + " > " + post);
          }
          bioSamplesClient.deleteCurationLink(cl.getContent());
        }
      }
    }
    return null;
  }
}
