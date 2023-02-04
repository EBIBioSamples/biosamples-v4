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
package uk.ac.ebi.biosamples.solr.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.ExternalReferenceService;
import uk.ac.ebi.biosamples.service.SampleRelationshipUtils;
import uk.ac.ebi.biosamples.solr.model.SolrSample;

@Service
public class SampleToSolrSampleConverter implements Converter<Sample, SolrSample> {

  private final ExternalReferenceService externalReferenceService;

  public SampleToSolrSampleConverter(ExternalReferenceService externalReferenceService) {
    this.externalReferenceService = externalReferenceService;
  }

  @Override
  public SolrSample convert(Sample sample) {
    Map<String, List<String>> attributeValues = new HashMap<>();
    Map<String, List<String>> attributeIris = new HashMap<>();
    Map<String, List<String>> attributeUnits = new HashMap<>();
    Map<String, List<String>> outgoingRelationships = new HashMap<>();
    Map<String, List<String>> incomingRelationships = new HashMap<>();
    Map<String, List<String>> externalReferencesData = new HashMap<>();
    List<String> keywords = new ArrayList<>();

    /* attributeValues.put(SolrFieldService.encodeFieldName("name"), Collections.singletonList(sample.getName().toLowerCase()));
    attributeValues.put(SolrFieldService.encodeFieldName("accession"), Collections.singletonList(sample.getAccession().toLowerCase()));*/

    if (sample.getCharacteristics() != null && sample.getCharacteristics().size() > 0) {

      for (Attribute attr : sample.getCharacteristics()) {

        final String key = SolrFieldService.encodeFieldName(attr.getType());
        // key = SolrSampleService.attributeTypeToField(key);

        String value = attr.getValue();
        // if its longer than 255 characters, don't add it to solr
        // solr cant index long things well, and its probably not useful for search
        if (value.length() > 255) {
          continue;
        }

        if (!attributeValues.containsKey(key)) {
          attributeValues.put(key, new ArrayList<>());
        }

        // if there is a unit, add it to the value for search & facet purposes
        if (attr.getUnit() != null) {
          value = value + " (" + attr.getUnit() + ")";
        }
        attributeValues.get(key).add(value);
        //        if (!value.equals(value.toLowerCase())) {
        //          attributeValues.get(key).add(value.toLowerCase());
        //        }

        // TODO this can't differentiate which iris go with which attribute if there
        // are multiple attributes with the same type
        if (!attributeIris.containsKey(key)) {
          attributeIris.put(key, new ArrayList<>());
        }
        if (attr.getIri().isEmpty()) {
          attributeIris.get(key).add("");
        } else {
          List<String> iris =
              attr.getIri().stream()
                  .map(iri -> getOntologyFromIri(iri))
                  .collect(Collectors.toList());
          attributeIris.get(key).addAll(iris);
          keywords.addAll(iris.stream().map(String::toLowerCase).collect(Collectors.toList()));
        }

        if (!attributeUnits.containsKey(key)) {
          attributeUnits.put(key, new ArrayList<>());
        }
        if (attr.getUnit() == null) {
          attributeUnits.get(key).add("");
        } else {
          attributeUnits.get(key).add(attr.getUnit());
        }
      }
    }

    //  Extract the abstract data type and add them as characteristics in solr
    sample
        .getData()
        .parallelStream()
        .forEach(
            abstractData -> {
              String key = SolrFieldService.encodeFieldName("structured data");

              if (!attributeValues.containsKey(key)) {
                attributeValues.put(key, new ArrayList<>());
              }

              attributeValues.get(key).add(abstractData.getDataType().name());
            });

    // turn external reference into additional attributes for facet & filter
    for (ExternalReference externalReference : sample.getExternalReferences()) {
      String externalReferenceNickname = externalReferenceService.getNickname(externalReference);
      String externalReferenceNicknameKey =
          SolrFieldService.encodeFieldName(externalReferenceNickname);
      String key = SolrFieldService.encodeFieldName("external reference");
      String keyDuo = SolrFieldService.encodeFieldName("data use conditions");

      if (!attributeValues.containsKey(key)) {
        attributeValues.put(key, new ArrayList<>());
      }
      attributeValues.get(key).add(externalReferenceNickname);

      if (externalReference.getDuo() != null && !externalReference.getDuo().isEmpty()) {
        if (!attributeValues.containsKey(keyDuo)) {
          attributeValues.put(keyDuo, new ArrayList<>());
        }
        attributeValues.get(keyDuo).addAll(externalReference.getDuo());
      }

      // Add the external reference data id
      Optional<String> externalReferenceDataId =
          externalReferenceService.getDataId(externalReference);
      if (externalReferenceDataId.isPresent()) {
        if (externalReferencesData == null) {
          externalReferencesData = new HashMap<>();
        }
        if (!externalReferencesData.containsKey(externalReferenceNicknameKey)) {
          externalReferencesData.put(externalReferenceNicknameKey, new ArrayList<>());
        }
        externalReferencesData.get(externalReferenceNicknameKey).add(externalReferenceDataId.get());
      }
    }

    // Add relationships owned by sample
    SortedSet<Relationship> sampleOutgoingRelationships =
        SampleRelationshipUtils.getOutgoingRelationships(sample);
    if (sampleOutgoingRelationships != null && !sampleOutgoingRelationships.isEmpty()) {
      String attributeValueKey = SolrFieldService.encodeFieldName("outgoing relationships");
      if (!attributeValues.containsKey(attributeValueKey)) {
        attributeValues.put(attributeValueKey, new ArrayList<>());
      }
      outgoingRelationships = new HashMap<>();
      for (Relationship rel : sampleOutgoingRelationships) {
        String key = SolrFieldService.encodeFieldName(rel.getType());
        if (!outgoingRelationships.containsKey(key)) {
          outgoingRelationships.put(key, new ArrayList<>());
        }
        outgoingRelationships.get(key).add(rel.getTarget());
        attributeValues.get(attributeValueKey).add(rel.getTarget());
      }
    }

    // Add relationships for which sample is the target
    SortedSet<Relationship> sampleIngoingRelationships =
        SampleRelationshipUtils.getIncomingRelationships(sample);
    if (sampleIngoingRelationships != null && !sampleIngoingRelationships.isEmpty()) {
      String attributeValueKey = SolrFieldService.encodeFieldName("incoming relationships");
      if (!attributeValues.containsKey(attributeValueKey)) {
        attributeValues.put(attributeValueKey, new ArrayList<>());
      }
      incomingRelationships = new HashMap<>();
      for (Relationship rel : sampleIngoingRelationships) {
        String key = SolrFieldService.encodeFieldName(rel.getType());
        if (!incomingRelationships.containsKey(key)) {
          incomingRelationships.put(key, new ArrayList<>());
        }
        incomingRelationships.get(key).add(rel.getSource());
        attributeValues.get(attributeValueKey).add(rel.getSource());
      }
    }

    String releaseSolr = DateTimeFormatter.ISO_INSTANT.format(sample.getRelease());
    String updateSolr = DateTimeFormatter.ISO_INSTANT.format(sample.getUpdate());

    sample
        .getOrganizations()
        .forEach(
            org -> {
              keywords.addAll(
                  Arrays.asList(org.getName(), org.getEmail(), org.getRole(), org.getUrl()));
            });

    sample
        .getContacts()
        .forEach(
            contact -> {
              keywords.addAll(
                  Arrays.asList(contact.getName(), contact.getAffiliation(), contact.getUrl()));
            });

    sample
        .getPublications()
        .forEach(
            pub -> {
              keywords.addAll(Arrays.asList(pub.getDoi(), pub.getPubMedId()));
            });

    return SolrSample.build(
        sample.getName(),
        sample.getAccession(),
        sample.getDomain(),
        sample.getWebinSubmissionAccountId(),
        releaseSolr,
        updateSolr,
        null,
        null,
        attributeValues,
        attributeIris,
        attributeUnits,
        outgoingRelationships,
        incomingRelationships,
        externalReferencesData,
        keywords);
  }

  private String getOntologyFromIri(String iri) {
    String[] iriParts = iri.split("/");
    return iriParts[iriParts.length - 1];
  }
}
