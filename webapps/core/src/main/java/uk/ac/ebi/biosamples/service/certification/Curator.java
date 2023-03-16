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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.certification.*;

@Service
public class Curator {
  private static final Logger LOG = LoggerFactory.getLogger(Curator.class);
  private static final Logger EVENTS = LoggerFactory.getLogger("events");
  private final ConfigLoader configLoader;
  private final Map<String, Plan> plansByCandidateChecklistID = new HashMap<>();
  private final Map<String, Recommendation> recommendationsByCertificationChecklistID =
      new HashMap<>();

  public Curator(final ConfigLoader configLoader) {
    this.configLoader = configLoader;
  }

  public List<PlanResult> runCurationPlans(final InterrogationResult interrogationResult) {
    final List<PlanResult> planResults = new ArrayList<>();
    if (interrogationResult == null) {
      final String message = "cannot run curation plans on null interrogation result";
      LOG.warn(message);
      throw new IllegalArgumentException(message);
    }

    if (interrogationResult.getSampleDocument() == null) {
      final String message = "cannot run curation plans on null sample";
      LOG.warn(message);
      throw new IllegalArgumentException(message);
    }

    for (final Checklist checklist : interrogationResult.getChecklists()) {
      final PlanResult planResult =
          runCurationPlan(checklist, interrogationResult.getSampleDocument());

      if (planResult != null) {
        LOG.info("Plan result added for checklist " + checklist.getID());
        planResults.add(planResult);
      }
    }

    return planResults;
  }

  private PlanResult runCurationPlan(
      final Checklist checklist, final SampleDocument sampleDocument) {
    final Plan plan = plansByCandidateChecklistID.get(checklist.getID());
    final PlanResult planResult = new PlanResult(sampleDocument, plan);
    if (plan == null) {
      EVENTS.info(
          String.format(
              "%s plan not found for %s", sampleDocument.getAccession(), checklist.getID()));
      return null;
    }
    if (plansByCandidateChecklistID.containsKey(checklist.getID())) {
      for (final Curation curation : plan.getCurations()) {
        final CurationResult curationResult = plan.applyCuration(sampleDocument, curation);
        if (curationResult != null) {
          planResult.addCurationResult(curationResult);
        }
      }
    }

    EVENTS.info(String.format("%s plan %s run", sampleDocument.getAccession(), plan.getID()));

    return planResult;
  }

  @PostConstruct
  public void init() {
    for (final Plan plan : configLoader.config.getPlans()) {
      plansByCandidateChecklistID.put(plan.getCandidateChecklistID(), plan);
    }

    for (final Recommendation recommendation : configLoader.config.getRecommendations()) {
      recommendationsByCertificationChecklistID.put(
          recommendation.getCertificationChecklistID(), recommendation);
    }
  }

  public List<Recommendation> runRecommendations(final InterrogationResult interrogationResult) {
    if (interrogationResult == null) {
      final String message = "cannot run curation plans on null interrogation result";
      LOG.warn(message);
      throw new IllegalArgumentException(message);
    }

    if (interrogationResult.getSampleDocument() == null) {
      final String message = "cannot run suggestion plans on null sample";
      LOG.warn(message);
      throw new IllegalArgumentException(message);
    }

    return recommendationsByCertificationChecklistID.keySet().stream()
        .filter(
            checklist ->
                !interrogationResult.getChecklists().stream()
                    .map(Checklist::getID)
                    .collect(Collectors.toList())
                    .contains(checklist))
        .map(checklist -> recommendationsByCertificationChecklistID.get(checklist))
        .collect(Collectors.toList());
  }
}
