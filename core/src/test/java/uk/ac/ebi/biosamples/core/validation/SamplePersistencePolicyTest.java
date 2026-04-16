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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.SubmittedViaType;

public class SamplePersistencePolicyTest {

  @Test
  public void returns_true_for_empty_stored_sample() {
    final Sample storedSample = new Sample.Builder("sample-empty").build();

    assertThat(SamplePersistencePolicy.isStoredSampleEmpty(storedSample)).isTrue();
  }

  @Test
  public void returns_false_for_stored_sample_with_metadata() {
    final Sample storedSample = new Sample.Builder("sample-with-taxid").withTaxId(9606L).build();

    assertThat(SamplePersistencePolicy.isStoredSampleEmpty(storedSample)).isFalse();
  }

  @Test
  public void file_uploader_superuser_with_accession_delegates_to_stored_sample_state() {
    final Sample newSample =
        new Sample.Builder("new-sample", "SAMEA123")
            .withSubmittedVia(SubmittedViaType.FILE_UPLOADER)
            .build();
    final Sample oldSample = new Sample.Builder("stored-sample").withTaxId(9606L).build();

    final boolean result = SamplePersistencePolicy.isStoredSampleEmpty(newSample, true, oldSample);

    assertThat(result).isFalse();
  }
}
