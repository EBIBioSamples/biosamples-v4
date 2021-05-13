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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.everit.json.schema.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.ac.ebi.biosamples.model.certification.*;
import uk.ac.ebi.biosamples.validation.ElixirSchemaValidator;
import uk.ac.ebi.biosamples.validation.ValidatorI;

@Service
public class Certifier {
  private static Logger LOG = LoggerFactory.getLogger(Certifier.class);
  private static Logger EVENTS = LoggerFactory.getLogger("events");

  private ConfigLoader configLoader;
  private ValidatorI validator;
  private Applicator applicator;

  public Certifier(ConfigLoader configLoader, @Qualifier("elixirValidator") ValidatorI validator, Applicator applicator) {
    this.validator = validator;
    this.configLoader = configLoader;
    this.applicator = applicator;
  }

  public CertificationResult certify(SampleDocument sampleDocument, boolean isJustCertification) {
    if (sampleDocument == null) {
      String message = "cannot certify a null sampleDocument";
      LOG.warn(message);
      throw new IllegalArgumentException(message);
    }

    return certify(sampleDocument, Collections.EMPTY_LIST, isJustCertification);
  }

  public CertificationResult certify(
      SampleDocument sampleDocument, boolean isJustCertification, String inputChecklist) {
    if (sampleDocument == null) {
      String message = "cannot certify a null sampleDocument";
      LOG.warn(message);
      throw new IllegalArgumentException(message);
    }

    return certify(sampleDocument, Collections.EMPTY_LIST, isJustCertification, inputChecklist);
  }

  private CertificationResult certify(
      SampleDocument sampleDocument,
      List<CurationResult> curationResults,
      boolean isJustCertification) {
    String accession = getAccession(sampleDocument);
    CertificationResult certificationResult = new CertificationResult(accession);

    String message;

    if (accession != null && !accession.isEmpty()) message = accession;
    else message = "New sample";

    boolean certified = false;
    String suggestionMessage = "";

    for (Checklist checklist : configLoader.config.getChecklists()) {
      try {
        validator.validate(checklist.getFileName(), sampleDocument.getDocument());
        EVENTS.info(
            String.format("%s validation successful against %s", message, checklist.getID()));
        certified = true;
        certificationResult.add(new Certificate(sampleDocument, curationResults, checklist));
        EVENTS.info(String.format("%s issued certificate %s", message, checklist.getID()));
      } catch (IOException ioe) {
        LOG.error(String.format("cannot open schema at %s", checklist.getFileName()), ioe);
      } catch (ValidationException ve) {
        EVENTS.info(String.format("%s validation failed against %s", message, checklist.getID()));

        if (!isJustCertification && checklist.isBlock()) {
          List<Recommendation> recommendations = configLoader.config.getRecommendations();
          List<Recommendation> matchedRecommendations = new ArrayList<>();
          List<Suggestion> matchedSuggestions = new ArrayList<>();

          if (recommendations != null && recommendations.size() > 0) {
            matchedRecommendations =
                configLoader.config.getRecommendations().stream()
                    .filter(
                        recommendation ->
                            recommendation.getCertificationChecklistID().equals(checklist.getID()))
                    .collect(Collectors.toList());
          }

          if (matchedRecommendations.size() > 0) {
            matchedSuggestions =
                matchedRecommendations.stream()
                    .map(Recommendation::getSuggestions)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
          }

          if (matchedSuggestions.size() > 0) {
            suggestionMessage =
                matchedSuggestions.stream()
                    .map(Suggestion::getComment)
                    .collect(Collectors.joining());
          }

          throw new SampleChecklistValidationFailureException(
              checklist.getID(), suggestionMessage, ve);
        }
      }
    }

    if (!certified) {
      EVENTS.info(String.format("%s not certified", message));
    }

    return certificationResult;
  }

  private String getAccession(SampleDocument sampleDocument) {
    return sampleDocument.getAccession();
  }

  private CertificationResult certify(
      SampleDocument sampleDocument,
      List<CurationResult> curationResults,
      boolean isJustCertification,
      String inputChecklist) {
    String accession = getAccession(sampleDocument);
    CertificationResult certificationResult = new CertificationResult(accession);

    String message;

    if (accession != null && !accession.isEmpty()) message = accession;
    else message = "New sample";

    boolean certified = false;
    String suggestionMessage = "";

    Optional<Checklist> filteredChecklist =
        configLoader.config.getChecklists().stream()
            .filter(checklist -> checklist.getName().equalsIgnoreCase(inputChecklist))
            .findAny();

    if (filteredChecklist.isPresent()) {
      Checklist checklist = filteredChecklist.get();

      try {
        validator.validate(checklist.getFileName(), sampleDocument.getDocument());
        EVENTS.info(
            String.format("%s validation successful against %s", message, checklist.getID()));
        certified = true;
        certificationResult.add(new Certificate(sampleDocument, curationResults, checklist));
        EVENTS.info(String.format("%s issued certificate %s", message, checklist.getID()));
      } catch (IOException ioe) {
        LOG.error(String.format("cannot open schema at %s", checklist.getFileName()), ioe);
      } catch (ValidationException ve) {
        EVENTS.info(String.format("%s validation failed against %s", message, checklist.getID()));

        if (!isJustCertification && checklist.isBlock()) {
          List<Recommendation> recommendations = configLoader.config.getRecommendations();
          List<Recommendation> matchedRecommendations = new ArrayList<>();
          List<Suggestion> matchedSuggestions = new ArrayList<>();

          if (recommendations != null && recommendations.size() > 0) {
            matchedRecommendations =
                configLoader.config.getRecommendations().stream()
                    .filter(
                        recommendation ->
                            recommendation.getCertificationChecklistID().equals(checklist.getID()))
                    .collect(Collectors.toList());
          }

          if (matchedRecommendations.size() > 0) {
            matchedSuggestions =
                matchedRecommendations.stream()
                    .map(Recommendation::getSuggestions)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
          }

          if (matchedSuggestions.size() > 0) {
            suggestionMessage =
                matchedSuggestions.stream()
                    .map(Suggestion::getComment)
                    .collect(Collectors.joining());
          }

          throw new SampleChecklistValidationFailureException(
              checklist.getID(), suggestionMessage, ve);
        }
      }
    } else {
      throw new SampleChecklistMissingException(inputChecklist);
    }

    if (!certified) {
      EVENTS.info(String.format("%s not certified", message));
    }

    return certificationResult;
  }

  public CertificationResult certify(
      HasCuratedSample hasCuratedSample, boolean isJustCertification) {
    if (hasCuratedSample == null) {
      String message = "cannot certify a null plan result";
      LOG.warn(message);
      throw new IllegalArgumentException(message);
    }

    return certify(
        applicator.apply(hasCuratedSample),
        hasCuratedSample.getCurationResults(),
        isJustCertification);
  }

  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public static class SampleChecklistValidationFailureException extends RuntimeException {
    public SampleChecklistValidationFailureException(
        String checklistDetails, String matchedSuggestionMessages, ValidationException ve) {
      super(
          "Sample failed validation against BioSamples minimal checklist "
              + checklistDetails
              + " and hence submission couldn't be completed."
              + " Reason - "
              + matchedSuggestionMessages,
          ve);
    }
  }

  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public static class SampleChecklistMissingException extends RuntimeException {
    public SampleChecklistMissingException(String inputChecklist) {
      super("Checklist by name " + inputChecklist + " doesn't exist");
    }
  }
}
