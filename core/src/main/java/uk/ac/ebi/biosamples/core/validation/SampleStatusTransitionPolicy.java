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

import java.util.EnumSet;
import java.util.Set;
import uk.ac.ebi.biosamples.core.model.SampleStatus;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;

public final class SampleStatusTransitionPolicy {
  private static final Set<SampleStatus> ALLOWED_PUBLIC_SOURCE_STATUSES =
      EnumSet.of(SampleStatus.PRIVATE);

  private SampleStatusTransitionPolicy() {}

  public static void validatePublicTransition(
      final SampleStatus oldStatus, final SampleStatus newStatus) {
    if (newStatus != SampleStatus.PUBLIC || oldStatus == null || oldStatus == SampleStatus.PUBLIC) {
      return;
    }

    if (!ALLOWED_PUBLIC_SOURCE_STATUSES.contains(oldStatus)) {
      throw new GlobalExceptions.InvalidSampleException();
    }
  }
}
