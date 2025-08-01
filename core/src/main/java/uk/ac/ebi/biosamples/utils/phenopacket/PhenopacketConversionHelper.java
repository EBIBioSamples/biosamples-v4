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
package uk.ac.ebi.biosamples.utils.phenopacket;

import java.util.Optional;
import org.phenopackets.schema.v1.core.OntologyClass;
import org.phenopackets.schema.v1.core.PhenotypicFeature;
import org.phenopackets.schema.v1.core.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.core.model.Attribute;

@Component
class PhenopacketConversionHelper {
  private static final Logger LOG = LoggerFactory.getLogger(PhenopacketConversionHelper.class);

  Optional<PhenopacketAttribute> convertAttribute(
      final String type, final String value, final String ontologyId, final String ontologyLabel) {
    return Optional.of(PhenopacketAttribute.build(type, value, ontologyId, ontologyLabel, false));
  }

  Optional<PhenopacketAttribute> convertAttributeWithNegation(
      final String type, final String value, final String ontologyId, final String ontologyLabel) {
    return Optional.of(PhenopacketAttribute.build(type, value, ontologyId, ontologyLabel, true));
  }

  Optional<PhenopacketAttribute> convertAttribute(final String type, final Attribute attribute) {
    Optional<PhenopacketAttribute> optionalPhenopacketAttribute = Optional.empty();
    if (!attribute.getIri().isEmpty()) {
      try {
        final OLSDataRetriever retriever = new OLSDataRetriever();
        final String firstIri = attribute.getIri().first();
        retriever.readOntologyJsonFromUrl(firstIri);
        optionalPhenopacketAttribute =
            Optional.of(
                PhenopacketAttribute.build(
                    type,
                    attribute.getValue(),
                    retriever.getOntologyTermId(),
                    retriever.getOntologyTermLabel()));
      } catch (final Exception e) {
        LOG.warn(
            "Failed to get IRI from OLS, possibly a wrong IRI format: {}",
            attribute.getIri().first(),
            e);
      }
    } else {
      optionalPhenopacketAttribute =
          Optional.of(PhenopacketAttribute.build(type, attribute.getValue(), null, null));
    }

    return optionalPhenopacketAttribute;
  }

  OntologyClass getOntology(final PhenopacketAttribute attribute) {
    return OntologyClass.newBuilder()
        .setId(attribute.getOntologyId())
        .setLabel(attribute.getOntologyLabel())
        .build();
  }

  PhenotypicFeature getPhenotype(final PhenopacketAttribute attribute) {
    return PhenotypicFeature.newBuilder()
        .setType(getOntology(attribute))
        .setNegated(attribute.isNegate())
        .setDescription(attribute.getValue())
        .build();
  }

  Optional<Resource> getResource(final PhenopacketAttribute attribute) {
    Optional<Resource> optionalResource = Optional.empty();
    if (attribute.getOntologyId() != null) {
      final String ontology = attribute.getOntologyId().split(":")[0];
      final OLSDataRetriever retriever = new OLSDataRetriever();
      retriever.readResourceInfoFromUrl(ontology);

      optionalResource =
          Optional.of(
              Resource.newBuilder()
                  .setId(retriever.getResourceId())
                  .setName(retriever.getResourceName())
                  .setUrl(retriever.getResourceUrl())
                  .setNamespacePrefix(retriever.getResourcePrefix())
                  .setVersion(retriever.getResourceVersion())
                  .build());
    }

    return optionalResource;
  }
}
