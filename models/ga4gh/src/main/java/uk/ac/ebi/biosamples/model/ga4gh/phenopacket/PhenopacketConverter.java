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
package uk.ac.ebi.biosamples.model.ga4gh.phenopacket;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import java.util.*;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class PhenopacketConverter {
  private static final Logger LOG = LoggerFactory.getLogger(PhenopacketConverter.class);
  private PhenopacketConversionHelper phenopacketConversionHelper;

  public PhenopacketConverter(PhenopacketConversionHelper phenopacketConversionHelper) {
    this.phenopacketConversionHelper = phenopacketConversionHelper;
  }

  public String convertToJsonPhenopacket(Sample sample) {
    Phenopacket phenopacket = convert(sample);
    String jsonPhenopacket = "";
    try {
      jsonPhenopacket = JsonFormat.printer().print(phenopacket);
    } catch (InvalidProtocolBufferException e) {
      LOG.error("Failed to convert to proto buff", e);
    }
    return jsonPhenopacket;
  }

  public Phenopacket convert(Sample sample) {
    List<PhenopacketAttribute> diseases = new ArrayList<>();
    Map<String, PhenopacketAttribute> attributes = new HashMap<>();
    normalizeAttributes(sample, attributes, diseases);

    return Phenopacket.newBuilder()
        .setId(sample.getAccession())
        .setMetaData(populateMetadata(attributes, diseases))
        .setSubject(populateSubject(sample, attributes))
        .addBiosamples(populateBiosample(sample, attributes))
        .addAllDiseases(populateDiseases(diseases))
        .build();
  }

  private MetaData populateMetadata(
      Map<String, PhenopacketAttribute> attributes, List<PhenopacketAttribute> diseases) {
    Set<Resource> resources = new HashSet<>();
    for (PhenopacketAttribute a : attributes.values()) {
      phenopacketConversionHelper.getResource(a).ifPresent(resources::add);
    }
    for (PhenopacketAttribute a : diseases) {
      phenopacketConversionHelper.getResource(a).ifPresent(resources::add);
    }

    return MetaData.newBuilder()
        .setCreated(Timestamp.newBuilder())
        .setCreatedBy("Biosamples phenopacket exporter")
        .addAllResources(resources)
        .build();
  }

  private Individual populateSubject(Sample sample, Map<String, PhenopacketAttribute> attributes) {
    Individual.Builder builder = Individual.newBuilder();
    builder.setId(sample.getAccession() + "-individual");
    if (attributes.containsKey("sex")) {
      Sex sex;
      if ("male".equalsIgnoreCase(attributes.get("sex").getValue())) {
        sex = Sex.MALE;
      } else if ("female".equalsIgnoreCase(attributes.get("sex").getValue())) {
        sex = Sex.FEMALE;
      } else {
        sex = Sex.UNKNOWN_SEX;
      }
      builder.setSex(sex);
    }
    if (attributes.containsKey("organism")) {
      builder.setTaxonomy(phenopacketConversionHelper.getOntology(attributes.get("organism")));
    }

    return builder.build();
  }

  private Biosample populateBiosample(Sample sample, Map<String, PhenopacketAttribute> attributes) {
    Biosample.Builder builder = Biosample.newBuilder();
    builder.setId(sample.getAccession());
    builder.setIndividualId(getIndividualId(sample));
    if (attributes.containsKey("organism")) {
      builder.setTaxonomy(phenopacketConversionHelper.getOntology(attributes.get("organism")));
    }
    if (attributes.containsKey("description")) {
      builder.setDescription(attributes.get("description").getValue());
    }
    if (attributes.containsKey("age")) {
      builder.setAgeOfIndividualAtCollection(
          Age.newBuilder().setAge(attributes.get("age").getValue()).build());
    }
    // phenotypic feature
    if (attributes.containsKey("phenotype")) {
      builder.addPhenotypicFeatures(
          phenopacketConversionHelper.getPhenotype(attributes.get("phenotype")));
    }
    if (attributes.containsKey("developmental stage")) {
      builder.addPhenotypicFeatures(
          phenopacketConversionHelper.getPhenotype(attributes.get("developmental stage")));
    }

    if (attributes.containsKey("tissue")) {
      builder.setSampledTissue(phenopacketConversionHelper.getOntology(attributes.get("tissue")));
    }
    if (attributes.containsKey("diagnosis")) {
      builder.setSampledTissue(
          phenopacketConversionHelper.getOntology(attributes.get("diagnosis")));
    }
    if (attributes.containsKey("tumor grade")) {
      builder.setSampledTissue(
          phenopacketConversionHelper.getOntology(attributes.get("tumor grade")));
    }
    if (attributes.containsKey("tumor progression")) {
      builder.setSampledTissue(
          phenopacketConversionHelper.getOntology(attributes.get("tumor progression")));
    }
    if (attributes.containsKey("biomarker")) {
      builder.setSampledTissue(
          phenopacketConversionHelper.getOntology(attributes.get("biomarker")));
    }
    if (attributes.containsKey("procedure")) {
      builder.setProcedure(
          Procedure.newBuilder()
              .setCode(phenopacketConversionHelper.getOntology(attributes.get("procedure")))
              .build());
    }
    if (attributes.containsKey("variants")) {
      builder.setSampledTissue(phenopacketConversionHelper.getOntology(attributes.get("variants")));
    }
    return builder.build();
  }

  private List<Disease> populateDiseases(List<PhenopacketAttribute> diseases) {
    List<Disease> diseaseList = new ArrayList<>();
    for (PhenopacketAttribute disease : diseases) {
      Disease.Builder builder = Disease.newBuilder();
      builder.setTerm(phenopacketConversionHelper.getOntology(disease));
      diseaseList.add(builder.build());
    }
    return diseaseList;
  }

  private void normalizeAttributes(
      Sample sample,
      Map<String, PhenopacketAttribute> attributeMap,
      List<PhenopacketAttribute> diseases) {
    for (Attribute attribute : sample.getAttributes()) {
      getAge(attribute).ifPresent(a -> addToMap(attributeMap, a));
      getDescription(attribute).ifPresent(a -> addToMap(attributeMap, a));
      getSex(attribute).ifPresent(a -> addToMap(attributeMap, a));

      if (!attribute.getIri().isEmpty()) {
        getOrganism(attribute).ifPresent(a -> addToMap(attributeMap, a));
        getDisease(attribute)
            .ifPresent(
                a -> {
                  addToMap(attributeMap, a);
                  diseases.add(a);
                });

        getPhenotypicFeatures(attribute).ifPresent(a -> addToMap(attributeMap, a));
        getSampleTissue(attribute).ifPresent(a -> addToMap(attributeMap, a));
        getDiagnosis(attribute).ifPresent(a -> addToMap(attributeMap, a));
        getTumorGrade(attribute).ifPresent(a -> addToMap(attributeMap, a));
        getTumorProgression(attribute).ifPresent(a -> addToMap(attributeMap, a));
        getBiomarker(attribute).ifPresent(a -> addToMap(attributeMap, a));
        getProcedure(attribute).ifPresent(a -> addToMap(attributeMap, a));
        getVariants(attribute).ifPresent(a -> addToMap(attributeMap, a));
      }

      extractDisease(attribute).ifPresent(diseases::add);
      extractMentalDisease(attribute).ifPresent(diseases::add);
    }
  }

  private String getIndividualId(Sample sample) {
    return sample.getAccession() + "-individual";
  }

  private Optional<PhenopacketAttribute> getSex(Attribute attribute) {
    Optional<PhenopacketAttribute> normalisedAttribute;
    if ("sex".equalsIgnoreCase(attribute.getType())
        || "gender".equalsIgnoreCase(attribute.getType())
        || "vioscreen gender".equalsIgnoreCase(attribute.getType())) {
      normalisedAttribute = phenopacketConversionHelper.convertAttribute("sex", attribute);
    } else {
      normalisedAttribute = Optional.empty();
    }
    return normalisedAttribute;
  }

  private Optional<PhenopacketAttribute> getOrganism(Attribute attribute) {
    Optional<PhenopacketAttribute> normalisedAttribute;
    if ("organism".equalsIgnoreCase(attribute.getType())
        || "species".equalsIgnoreCase(attribute.getType())) {
      normalisedAttribute = phenopacketConversionHelper.convertAttribute("organism", attribute);
    } else {
      normalisedAttribute = Optional.empty();
    }
    return normalisedAttribute;
  }

  private Optional<PhenopacketAttribute> getDisease(Attribute attribute) {
    Optional<PhenopacketAttribute> normalisedAttribute;
    if ("disease".equalsIgnoreCase(attribute.getType())
        || "disease state".equalsIgnoreCase(attribute.getType())) {
      normalisedAttribute = phenopacketConversionHelper.convertAttribute("disease", attribute);
    } else {
      normalisedAttribute = Optional.empty();
    }
    return normalisedAttribute;
  }

  private Optional<PhenopacketAttribute> getAge(Attribute attribute) {
    Optional<PhenopacketAttribute> normalisedAttribute;
    if ("age".equalsIgnoreCase(attribute.getType())
        || "age_years".equalsIgnoreCase(attribute.getType())
        || "age(years)".equalsIgnoreCase(attribute.getType())
        || "age at collection months".equalsIgnoreCase(attribute.getType())
        || "age at collection".equalsIgnoreCase(attribute.getType())
        || "age at collection mo".equalsIgnoreCase(attribute.getType())
        || "age at sample months".equalsIgnoreCase(attribute.getType())
        || "age at collection (months)".equalsIgnoreCase(attribute.getType())
        || "age at sampling".equalsIgnoreCase(attribute.getType())) {
      normalisedAttribute = phenopacketConversionHelper.convertAttribute("age", attribute);
    } else {
      normalisedAttribute = Optional.empty();
    }
    return normalisedAttribute;
  }

  private Optional<PhenopacketAttribute> getDescription(Attribute attribute) {
    Optional<PhenopacketAttribute> normalisedAttribute;
    if ("description".equalsIgnoreCase(attribute.getType())
        || "description title".equalsIgnoreCase(attribute.getType())) {
      normalisedAttribute = phenopacketConversionHelper.convertAttribute("description", attribute);
    } else {
      normalisedAttribute = Optional.empty();
    }
    return normalisedAttribute;
  }

  private Optional<PhenopacketAttribute> getPhenotypicFeatures(Attribute attribute) {
    Optional<PhenopacketAttribute> normalisedAttribute;
    if ("phenotype".equalsIgnoreCase(attribute.getType())) {
      normalisedAttribute = phenopacketConversionHelper.convertAttribute("phenotype", attribute);
    } else if ("development stage".equalsIgnoreCase(attribute.getType())
        || "developmental stage".equalsIgnoreCase(attribute.getType())
        || "dev stage".equalsIgnoreCase(attribute.getType())) {
      normalisedAttribute =
          phenopacketConversionHelper.convertAttribute("developmental stage", attribute);
    } else {
      normalisedAttribute = Optional.empty();
    }
    return normalisedAttribute;
  }

  private Optional<PhenopacketAttribute> getSampleTissue(Attribute attribute) {
    Optional<PhenopacketAttribute> normalisedAttribute;
    if ("tissue".equalsIgnoreCase(attribute.getType())) {
      normalisedAttribute = phenopacketConversionHelper.convertAttribute("tissue", attribute);
    } else {
      normalisedAttribute = Optional.empty();
    }
    return normalisedAttribute;
  }

  private Optional<PhenopacketAttribute> getDiagnosis(Attribute attribute) {
    Optional<PhenopacketAttribute> normalisedAttribute;
    if ("histological diagnosis".equalsIgnoreCase(attribute.getType())
        || "diagnosis".equalsIgnoreCase(attribute.getType())) {
      normalisedAttribute = phenopacketConversionHelper.convertAttribute("diagnosis", attribute);
    } else {
      normalisedAttribute = Optional.empty();
    }
    return normalisedAttribute;
  }

  private Optional<PhenopacketAttribute> getTumorGrade(Attribute attribute) {
    Optional<PhenopacketAttribute> normalisedAttribute;
    if ("tumor grade".equalsIgnoreCase(attribute.getType())
        || "tumor stage".equalsIgnoreCase(attribute.getType())
        || "tumor grading".equalsIgnoreCase(attribute.getType())) {
      normalisedAttribute = phenopacketConversionHelper.convertAttribute("tumor grade", attribute);
    } else {
      normalisedAttribute = Optional.empty();
    }
    return normalisedAttribute;
  }

  private Optional<PhenopacketAttribute> getTumorProgression(Attribute attribute) {
    Optional<PhenopacketAttribute> normalisedAttribute;
    if ("tumor progression".equalsIgnoreCase(attribute.getType())) {
      normalisedAttribute =
          phenopacketConversionHelper.convertAttribute("tumor progression", attribute);
    } else {
      normalisedAttribute = Optional.empty();
    }
    return normalisedAttribute;
  }

  private Optional<PhenopacketAttribute> getBiomarker(Attribute attribute) {
    Optional<PhenopacketAttribute> normalisedAttribute;
    if ("biomarker".equalsIgnoreCase(attribute.getType())) {
      normalisedAttribute = phenopacketConversionHelper.convertAttribute("biomarker", attribute);
    } else {
      normalisedAttribute = Optional.empty();
    }
    return normalisedAttribute;
  }

  private Optional<PhenopacketAttribute> getProcedure(Attribute attribute) {
    Optional<PhenopacketAttribute> normalisedAttribute;
    if ("procedure".equalsIgnoreCase(attribute.getType())) {
      normalisedAttribute = phenopacketConversionHelper.convertAttribute("procedure", attribute);
    } else {
      normalisedAttribute = Optional.empty();
    }
    return normalisedAttribute;
  }

  private Optional<PhenopacketAttribute> getVariants(Attribute attribute) {
    Optional<PhenopacketAttribute> normalisedAttribute;
    if ("variants".equalsIgnoreCase(attribute.getType())) {
      normalisedAttribute = phenopacketConversionHelper.convertAttribute("variants", attribute);
    } else {
      normalisedAttribute = Optional.empty();
    }
    return normalisedAttribute;
  }

  private Optional<PhenopacketAttribute> getOtherFields(Attribute attribute) {
    Optional<PhenopacketAttribute> normalisedAttribute = Optional.empty();
    if ("tissue".equalsIgnoreCase(attribute.getType())) {
      normalisedAttribute = phenopacketConversionHelper.convertAttribute("tissue", attribute);
    }

    return normalisedAttribute;
  }

  private Optional<PhenopacketAttribute> extractDisease(Attribute attribute) {
    Optional<PhenopacketAttribute> normalisedAttribute = Optional.empty();
    if ("diabetes".equalsIgnoreCase(attribute.getType())
        || "diabetes type".equalsIgnoreCase(attribute.getType())) {
      if ("no".equalsIgnoreCase(attribute.getValue())
          || "false".equalsIgnoreCase(attribute.getValue())) {
        normalisedAttribute =
            phenopacketConversionHelper.convertAttributeWithNegation(
                "disease", "diabetes", "MONDO:0005015", "diabetes");
      } else {
        normalisedAttribute =
            phenopacketConversionHelper.convertAttribute(
                "disease", "diabetes", "MONDO:0005015", "diabetes");
      }
    } else if ("lung disease".equalsIgnoreCase(attribute.getType())) {
      if ("no".equalsIgnoreCase(attribute.getValue())
          || "false".equalsIgnoreCase(attribute.getValue())) {
        normalisedAttribute =
            phenopacketConversionHelper.convertAttributeWithNegation(
                "disease", "lung disease", "MONDO:0005275", "lung disease");
      } else {
        normalisedAttribute =
            phenopacketConversionHelper.convertAttribute(
                "disease", "lung disease", "MONDO:0005275", "lung disease");
      }
    } else if ("liver disease".equalsIgnoreCase(attribute.getType())) {
      if ("no".equalsIgnoreCase(attribute.getValue())
          || "false".equalsIgnoreCase(attribute.getValue())) {
        normalisedAttribute =
            phenopacketConversionHelper.convertAttributeWithNegation(
                "disease", "liver disease", "MONDO:0005154", "liver disease");
      } else {
        normalisedAttribute =
            phenopacketConversionHelper.convertAttribute(
                "disease", "liver disease", "MONDO:0005154", "liver disease");
      }
    } else if ("kidney disease".equalsIgnoreCase(attribute.getType())) {
      if ("no".equalsIgnoreCase(attribute.getValue())
          || "false".equalsIgnoreCase(attribute.getValue())) {
        normalisedAttribute =
            phenopacketConversionHelper.convertAttributeWithNegation(
                "disease", "kidney disease", "MONDO:0005240", "kidney disease");
      } else {
        normalisedAttribute =
            phenopacketConversionHelper.convertAttribute(
                "disease", "kidney disease", "MONDO:0005240", "kidney disease");
      }
    } else if ("cardiovascular disease".equalsIgnoreCase(attribute.getType())) {
      if ("no".equalsIgnoreCase(attribute.getValue())
          || "false".equalsIgnoreCase(attribute.getValue())) {
        normalisedAttribute =
            phenopacketConversionHelper.convertAttributeWithNegation(
                "disease", "cardiovascular disease", "MONDO:0004995", "cardiovascular disease");
      } else {
        normalisedAttribute =
            phenopacketConversionHelper.convertAttribute(
                "disease", "cardiovascular disease", "MONDO:0004995", "cardiovascular disease");
      }
    } else if ("cancer".equalsIgnoreCase(attribute.getType())) {
      if ("no".equalsIgnoreCase(attribute.getValue())
          || "false".equalsIgnoreCase(attribute.getValue())) {
        normalisedAttribute =
            phenopacketConversionHelper.convertAttributeWithNegation(
                "disease", "cancer", "MONDO:0004992", "cancer");
      } else {
        normalisedAttribute =
            phenopacketConversionHelper.convertAttribute(
                "disease", "cancer", "MONDO:0004992", "cancer");
      }
    } else if ("ibd".equalsIgnoreCase(attribute.getType())) {
      if ("no".equalsIgnoreCase(attribute.getValue())
          || "false".equalsIgnoreCase(attribute.getValue())) {
        normalisedAttribute =
            phenopacketConversionHelper.convertAttributeWithNegation(
                "disease", "ibd", "MONDO:0005265", "ibd");
      } else {
        normalisedAttribute =
            phenopacketConversionHelper.convertAttribute("disease", "ibd", "MONDO:0005265", "ibd");
      }
    }

    return normalisedAttribute;
  }

  private Optional<PhenopacketAttribute> extractMentalDisease(Attribute attribute) {
    Optional<PhenopacketAttribute> normalisedAttribute = Optional.empty();
    if ("mental illness".equalsIgnoreCase(attribute.getType())) {
      if ("yes".equalsIgnoreCase(attribute.getValue())
          || "true".equalsIgnoreCase(attribute.getValue())) {
        normalisedAttribute =
            phenopacketConversionHelper.convertAttribute(
                "disease", "mental illness", "MONDO:0002025", "psychiatric disorder");
      }
    } else if (attribute.getType().toLowerCase().startsWith("mental illness type")) {
      if (attribute.getType().toLowerCase().contains("substance abuse")) {
        if ("yes".equalsIgnoreCase(attribute.getValue())
            || "true".equalsIgnoreCase(attribute.getValue())) {
          normalisedAttribute =
              phenopacketConversionHelper.convertAttribute(
                  "disease", "substance abuse", "MONDO:0002491", "substance abuse");
        } else if ("no".equalsIgnoreCase(attribute.getValue())
            || "false".equalsIgnoreCase(attribute.getValue())) {
          normalisedAttribute =
              phenopacketConversionHelper.convertAttributeWithNegation(
                  "disease", "substance abuse", "MONDO:0002491", "substance abuse");
        }
      } else if (attribute.getType().toLowerCase().contains("anorexia nervosa")) {
        if ("yes".equalsIgnoreCase(attribute.getValue())
            || "true".equalsIgnoreCase(attribute.getValue())) {
          normalisedAttribute =
              phenopacketConversionHelper.convertAttribute(
                  "disease", "anorexia nervosa", "MONDO:0005351", "anorexia nervosa");
        } else if ("no".equalsIgnoreCase(attribute.getValue())
            || "false".equalsIgnoreCase(attribute.getValue())) {
          normalisedAttribute =
              phenopacketConversionHelper.convertAttributeWithNegation(
                  "disease", "anorexia nervosa", "MONDO:0005351", "anorexia nervosa");
        }
      } else if (attribute.getType().toLowerCase().contains("schizophrenia")) {
        if ("yes".equalsIgnoreCase(attribute.getValue())
            || "true".equalsIgnoreCase(attribute.getValue())) {
          normalisedAttribute =
              phenopacketConversionHelper.convertAttribute(
                  "disease", "schizophrenia", "MONDO:0005090", "schizophrenia");
        } else if ("no".equalsIgnoreCase(attribute.getValue())
            || "false".equalsIgnoreCase(attribute.getValue())) {
          normalisedAttribute =
              phenopacketConversionHelper.convertAttributeWithNegation(
                  "disease", "schizophrenia", "MONDO:0005090", "schizophrenia");
        }
      } else if (attribute.getType().toLowerCase().contains("depression")) {
        if ("yes".equalsIgnoreCase(attribute.getValue())
            || "true".equalsIgnoreCase(attribute.getValue())) {
          normalisedAttribute =
              phenopacketConversionHelper.convertAttribute(
                  "disease", "depression", "MONDO:0002050", "depression");
        } else if ("no".equalsIgnoreCase(attribute.getValue())
            || "false".equalsIgnoreCase(attribute.getValue())) {
          normalisedAttribute =
              phenopacketConversionHelper.convertAttributeWithNegation(
                  "disease", "depression", "MONDO:0002050", "depression");
        }
      } else if (attribute.getType().toLowerCase().contains("bulimia nervosa")) {
        if ("yes".equalsIgnoreCase(attribute.getValue())
            || "true".equalsIgnoreCase(attribute.getValue())) {
          normalisedAttribute =
              phenopacketConversionHelper.convertAttribute(
                  "disease", "bulimia nervosa", "MONDO:0005452", "bulimia nervosa");
        } else if ("no".equalsIgnoreCase(attribute.getValue())
            || "false".equalsIgnoreCase(attribute.getValue())) {
          normalisedAttribute =
              phenopacketConversionHelper.convertAttributeWithNegation(
                  "disease", "bulimia nervosa", "MONDO:0005452", "bulimia nervosa");
        }
      } else if (attribute.getType().toLowerCase().contains("bipolar disorder")) {
        if ("yes".equalsIgnoreCase(attribute.getValue())
            || "true".equalsIgnoreCase(attribute.getValue())) {
          normalisedAttribute =
              phenopacketConversionHelper.convertAttribute(
                  "disease", "bipolar disorder", "MONDO:0004985", "bipolar disorder");
        } else if ("no".equalsIgnoreCase(attribute.getValue())
            || "false".equalsIgnoreCase(attribute.getValue())) {
          normalisedAttribute =
              phenopacketConversionHelper.convertAttributeWithNegation(
                  "disease", "bipolar disorder", "MONDO:0004985", "bipolar disorder");
        }
      }
    } else if ("study disease".equalsIgnoreCase(attribute.getType())) {
      // todo check attribute 'subject is affected'
      normalisedAttribute = phenopacketConversionHelper.convertAttribute("disease", attribute);
    } else if ("study name".equalsIgnoreCase(attribute.getType())) {
      // todo check attribute 'subject is affected'
      normalisedAttribute = phenopacketConversionHelper.convertAttribute("disease", attribute);
    }

    return normalisedAttribute;
  }

  private void addToMap(Map<String, PhenopacketAttribute> m, PhenopacketAttribute a) {
    m.put(a.getType(), a);
  }
}
