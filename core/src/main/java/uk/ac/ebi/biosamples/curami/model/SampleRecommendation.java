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
package uk.ac.ebi.biosamples.curami.model;

import uk.ac.ebi.biosamples.core.model.Sample;

public class SampleRecommendation {
  private CuramiRecommendation recommendations;
  private Sample sample;

  public SampleRecommendation(CuramiRecommendation recommendations, Sample sample) {
    this.recommendations = recommendations;
    this.sample = sample;
  }

  public CuramiRecommendation getRecommendations() {
    return recommendations;
  }

  public Sample getSample() {
    return sample;
  }
}
