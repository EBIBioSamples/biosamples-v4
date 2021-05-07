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
package uk.ac.ebi.biosamples.mongo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Certificate;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoCertificate;
import uk.ac.ebi.biosamples.mongo.model.MongoExternalReference;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class to convert {@link Sample} to {@link MongoSample} without validating if sample has a domain.
 * This is useful for structured data PUT requests where we don't enforce user to have a domain in
 * the {@link Sample}
 */
@Service
public class SampleToMongoSampleStructuredDataCentricConverter {
  @Autowired
  private ExternalReferenceToMongoExternalReferenceConverter
      externalReferenceToMongoExternalReferenceConverter;

  @Autowired
  private RelationshipToMongoRelationshipConverter relationshipToMongoRelationshipConverter;

  @Autowired private CertificateToMongoCertificateConverter certificateToMongoCertificateConverter;

  private static Logger LOGGER = LoggerFactory.getLogger(MongoSampleToSampleConverter.class);

  public MongoSample convert(Sample sample, String authProvider) {
    SortedSet<MongoExternalReference> externalReferences = new TreeSet<>();
    for (ExternalReference mongoExternalReference : sample.getExternalReferences()) {
      externalReferences.add(
          externalReferenceToMongoExternalReferenceConverter.convert(mongoExternalReference));
    }

    SortedSet<MongoRelationship> relationships = new TreeSet<>();
    for (Relationship relationship : sample.getRelationships()) {
      relationships.add(relationshipToMongoRelationshipConverter.convert(relationship));
    }

    SortedSet<MongoCertificate> certificates = new TreeSet<>();
    for (Certificate certificate : sample.getCertificates()) {
      certificates.add(certificateToMongoCertificateConverter.convert(certificate));
    }

    if (authProvider.equals("WEBIN")) {
      sample
              .getData()
              .forEach(
                      data -> {
                        if (data.getWebinSubmissionAccountId() == null || data.getWebinSubmissionAccountId().isEmpty()) {
                          LOGGER.warn(
                                  String.format(
                                          "sample %s structured data does not have a webin id", sample.getAccession()));
                          throw new RuntimeException("sample structured data does not have a webin id " + sample);
                        }
                      });
    } else {
      sample
              .getData()
              .forEach(
                      data -> {
                        if (data.getDomain() == null || data.getDomain().isEmpty()) {
                          LOGGER.warn(
                                  String.format(
                                          "sample %s structured data does not have a domain", sample.getAccession()));
                          throw new RuntimeException("sample structured data does not have a domain " + sample);
                        }
                      });
    }

    return MongoSample.build(
            sample.getName(),
            sample.getAccession(),
            sample.getDomain(),
            sample.getWebinSubmissionAccountId(),
            sample.getRelease(),
            sample.getUpdate(),
            sample.getCreate(),
            sample.getSubmitted(),
            sample.getReviewed(),
            sample.getCharacteristics(),
            sample.getData(),
            relationships,
            externalReferences,
            sample.getOrganizations(),
            sample.getContacts(),
            sample.getPublications(),
            certificates,
            sample.getSubmittedVia());
  }
}
