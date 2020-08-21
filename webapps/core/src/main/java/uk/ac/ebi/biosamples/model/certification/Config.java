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
package uk.ac.ebi.biosamples.model.certification;

import java.util.List;

public class Config {
  private List<Checklist> checklists;

  private List<Plan> plans;

  private List<Recommendation> recommendations;

  public List<Recommendation> getRecommendations() {
    return recommendations;
  }

  public void setRecommendations(List<Recommendation> recommendations) {
    this.recommendations = recommendations;
  }

  public List<Checklist> getChecklists() {
    return checklists;
  }

  public void setChecklists(List<Checklist> checklists) {
    this.checklists = checklists;
  }

  public List<Plan> getPlans() {
    return plans;
  }

  public void setPlans(List<Plan> plans) {
    this.plans = plans;
  }

  public Config() {}
}
