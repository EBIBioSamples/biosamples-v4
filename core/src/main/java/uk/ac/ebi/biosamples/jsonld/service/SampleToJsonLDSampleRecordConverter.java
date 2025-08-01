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
package uk.ac.ebi.biosamples.jsonld.service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.springframework.core.convert.converter.Converter;
import uk.ac.ebi.biosamples.core.model.Attribute;
import uk.ac.ebi.biosamples.core.model.ExternalReference;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.jsonld.model.JsonLDDataRecord;
import uk.ac.ebi.biosamples.jsonld.model.JsonLDDefinedTerm;
import uk.ac.ebi.biosamples.jsonld.model.JsonLDPropertyValue;
import uk.ac.ebi.biosamples.jsonld.model.JsonLDSample;

public class SampleToJsonLDSampleRecordConverter implements Converter<Sample, JsonLDDataRecord> {

  @Override
  public JsonLDDataRecord convert(final Sample sample) {

    final JsonLDDataRecord sampleRecord = new JsonLDDataRecord();

    sampleRecord.dateCreated(sample.getCreate().atZone(ZoneId.of("UTC")));
    sampleRecord.dateReleased(sample.getRelease().atZone(ZoneId.of("UTC")));
    sampleRecord.dateModified(sample.getUpdate().atZone(ZoneId.of("UTC")));

    if (sample.getSubmitted() != null) {
      sampleRecord.dateSubmitted(sample.getSubmitted().atZone(ZoneId.of("UTC")));
    }

    final JsonLDSample jsonLD = new JsonLDSample();
    final String[] identifiers = {getBioSamplesIdentifierDotOrg(sample.getAccession())};
    jsonLD.setSameAs(getBioSamplesIdentifierDotOrgLink(sample.getAccession()));
    jsonLD.setIdentifiers(identifiers);
    jsonLD.setName(sample.getName());

    final List<JsonLDPropertyValue> jsonLDAttributeList = getAttributeList(sample);
    if (!jsonLDAttributeList.isEmpty()) {
      final Optional<JsonLDPropertyValue> optionalDescription =
          jsonLDAttributeList.stream()
              .filter(attr -> attr.getName().equalsIgnoreCase("description"))
              .findFirst();
      if (optionalDescription.isPresent()) {
        final JsonLDPropertyValue description = optionalDescription.get();
        jsonLD.setDescription(description.getValue());
        jsonLDAttributeList.remove(description);
      }
      jsonLD.setAdditionalProperties(jsonLDAttributeList);
    }

    final List<String> datasets = getDatasets(sample);
    if (!datasets.isEmpty()) {
      jsonLD.setSubjectOf(datasets);
    }

    sampleRecord.identifier(getBioSamplesIdentifierDotOrg(sample.getAccession()));
    sampleRecord.mainEntity(jsonLD);

    return sampleRecord;
  }

  private List<JsonLDPropertyValue> getAttributeList(final Sample sample) {
    final Iterator<Attribute> attributesIterator = sample.getAttributes().iterator();
    final List<JsonLDPropertyValue> jsonLDAttributeList = new ArrayList<>();
    while (attributesIterator.hasNext()) {
      final Attribute attr = attributesIterator.next();
      final JsonLDPropertyValue pv = new JsonLDPropertyValue();
      pv.setName(attr.getType());
      pv.setValue(attr.getValue());
      if (!attr.getIri().isEmpty()) {
        // this only puts the first IRI in
        //                JsonLDMedicalCode medicalCode = new JsonLDMedicalCode();
        //                medicalCode.setTermCode(attr.getIri().iterator().next());

        // Assuming that if the iri is not starting with a http[s] is
        // probably a CURIE
        final List<JsonLDDefinedTerm> valueReferences = new ArrayList<>();
        for (final String iri : attr.getIri()) {
          final JsonLDDefinedTerm valueReference = new JsonLDDefinedTerm();
          if (iri.matches("^https?://.*")) {
            valueReference.setId(iri);
          } else {
            valueReference.setTermCode(iri);
          }
          valueReferences.add(valueReference);
        }
        pv.setValueReference(valueReferences);
      }
      jsonLDAttributeList.add(pv);
    }
    return jsonLDAttributeList;
  }

  private List<String> getDatasets(final Sample sample) {
    final Iterator<ExternalReference> externalRefsIterator =
        sample.getExternalReferences().iterator();
    final List<String> datasets = new ArrayList<>();
    while (externalRefsIterator.hasNext()) {
      final ExternalReference externalReference = externalRefsIterator.next();
      datasets.add(externalReference.getUrl());
    }
    return datasets;
  }

  private String getBioSamplesIdentifierDotOrg(final String accession) {
    return "biosample:" + accession;
  }

  // TODO change identifiers
  private String getBioSamplesIdentifierDotOrgLink(final String accession) {
    return "http://identifiers.org/biosample/" + accession;
  }
}
