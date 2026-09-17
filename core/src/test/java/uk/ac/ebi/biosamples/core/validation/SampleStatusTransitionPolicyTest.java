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

import org.junit.Test;
import uk.ac.ebi.biosamples.core.model.SampleStatus;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;

public class SampleStatusTransitionPolicyTest {

  @Test
  public void allows_private_to_public_transition() {
    SampleStatusTransitionPolicy.validatePublicTransition(
        SampleStatus.PRIVATE, SampleStatus.PUBLIC);
  }

  @Test
  public void allows_non_public_target_without_validation() {
    SampleStatusTransitionPolicy.validatePublicTransition(
        SampleStatus.SUPPRESSED, SampleStatus.SUPPRESSED);
  }

  @Test(expected = GlobalExceptions.InvalidSampleException.class)
  public void rejects_suppressed_to_public_transition() {
    SampleStatusTransitionPolicy.validatePublicTransition(
        SampleStatus.SUPPRESSED, SampleStatus.PUBLIC);
  }
}
