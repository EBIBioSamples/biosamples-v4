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
package uk.ac.ebi.biosamples.mongo.service;

import java.time.Instant;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
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

@Service
public class MongoSampleToSampleConverter implements Function<MongoSample, Sample> {
  @Autowired
  private MongoExternalReferenceToExternalReferenceConverter
      mongoExternalReferenceToExternalReferenceConverter;

  @Autowired
  private MongoRelationshipToRelationshipConverter mongoRelationshipToRelationshipConverter;

  @Autowired private MongoCertificateToCertificateConverter mongoCertificateToCertificateConverter;
  private static final Logger LOGGER = LoggerFactory.getLogger(MongoSampleToSampleConverter.class);

  @Override
  public Sample apply(final MongoSample mongoSample) {
    final Sample convertedSample;
    final SortedSet<ExternalReference> externalReferences = new TreeSet<>();

    if (mongoSample.getExternalReferences() != null
        && mongoSample.getExternalReferences().size() > 0) {
      for (final MongoExternalReference mongoExternalReference :
          mongoSample.getExternalReferences()) {
        if (mongoExternalReference != null) {
          externalReferences.add(
              mongoExternalReferenceToExternalReferenceConverter.convert(mongoExternalReference));
        }
      }
    }

    final SortedSet<Relationship> relationships = new TreeSet<>();

    if (mongoSample.getRelationships() != null && mongoSample.getRelationships().size() > 0) {
      for (final MongoRelationship mongoRelationship : mongoSample.getRelationships()) {
        if (mongoRelationship != null) {
          relationships.add(mongoRelationshipToRelationshipConverter.convert(mongoRelationship));
        }
      }
    }

    final SortedSet<Certificate> certificates = new TreeSet<>();

    if (mongoSample.getCertificates() != null && mongoSample.getCertificates().size() > 0) {
      for (final MongoCertificate certificate : mongoSample.getCertificates()) {
        if (certificate != null) {
          certificates.add(mongoCertificateToCertificateConverter.convert(certificate));
        }
      }
    }

    // when we convert to a Sample then the MongoSample *must* have a domain or a Webin ID
    if (mongoSample.getDomain() == null && mongoSample.getWebinSubmissionAccountId() == null) {
      LOGGER.warn(
          String.format(
              "Sample %s does not have a domain or a WEBIN submission account ID",
              mongoSample.getAccession()));
      throw new RuntimeException("Sample does not have domain or a WEBIN submission account ID");
    }

    final Instant submitted = mongoSample.getSubmitted();

    LOGGER.info("SAMPLE STATUS IN CONVERTER IS " + mongoSample.getStatus());

    if (submitted == null) {
      convertedSample =
          new Sample.Builder(mongoSample.getName(), mongoSample.getAccession())
              .withDomain(mongoSample.getDomain())
              .withTaxId(mongoSample.getTaxId())
              .withStatus(mongoSample.getStatus())
              .withWebinSubmissionAccountId(mongoSample.getWebinSubmissionAccountId())
              .withRelease(mongoSample.getRelease())
              .withUpdate(mongoSample.getUpdate())
              .withCreate(mongoSample.getCreate())
              .withNoSubmitted()
              .withAttributes(mongoSample.getAttributes())
              .withRelationships(relationships)
              .withData(mongoSample.getData())
              .withExternalReferences(externalReferences)
              .withOrganizations(mongoSample.getOrganizations())
              .withContacts(mongoSample.getContacts())
              .withPublications(mongoSample.getPublications())
              .withCertificates(certificates)
              .withSubmittedVia(mongoSample.getSubmittedVia())
              .build();
    } else {
      convertedSample =
          new Sample.Builder(mongoSample.getName(), mongoSample.getAccession())
              .withDomain(mongoSample.getDomain())
              .withTaxId(mongoSample.getTaxId())
              .withStatus(mongoSample.getStatus())
              .withWebinSubmissionAccountId(mongoSample.getWebinSubmissionAccountId())
              .withRelease(mongoSample.getRelease())
              .withUpdate(mongoSample.getUpdate())
              .withCreate(mongoSample.getCreate())
              .withSubmitted(mongoSample.getSubmitted())
              .withAttributes(mongoSample.getAttributes())
              .withRelationships(relationships)
              .withData(mongoSample.getData())
              .withExternalReferences(externalReferences)
              .withOrganizations(mongoSample.getOrganizations())
              .withContacts(mongoSample.getContacts())
              .withPublications(mongoSample.getPublications())
              .withCertificates(certificates)
              .withSubmittedVia(mongoSample.getSubmittedVia())
              .build();
    }

    final Instant reviewed = mongoSample.getReviewed();

    if (reviewed == null) {
      return Sample.Builder.fromSample(convertedSample).withNoReviewed().build();
    } else {
      return Sample.Builder.fromSample(convertedSample)
          .withReviewed(mongoSample.getReviewed())
          .build();
    }
  }
}
