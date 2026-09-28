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
package uk.ac.ebi.biosamples.core.validation;

import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.SubmittedViaType;

public final class SamplePersistencePolicy {
  private SamplePersistencePolicy() {}

  public static boolean isStoredSampleEmpty(
      final Sample newSample, final boolean isWebinSuperUser, final Sample oldSample) {
    if (isWebinSuperUser) {
      if (newSample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
        if (newSample.hasAccession()) {
          return isStoredSampleEmpty(oldSample);
        }

        return true;
      }

      return false;
    }

    if (newSample.hasAccession()) {
      return isStoredSampleEmpty(oldSample);
    }

    return true;
  }

  public static boolean isStoredSampleEmpty(final Sample oldSample) {
    return (oldSample.getTaxId() == null || oldSample.getTaxId() <= 0)
        && oldSample.getAttributes().isEmpty()
        && oldSample.getRelationships().isEmpty()
        && oldSample.getPublications().isEmpty()
        && oldSample.getContacts().isEmpty()
        && oldSample.getOrganizations().isEmpty()
        && oldSample.getData().isEmpty()
        && oldSample.getExternalReferences().isEmpty()
        && oldSample.getStructuredData().isEmpty();
  }
}
