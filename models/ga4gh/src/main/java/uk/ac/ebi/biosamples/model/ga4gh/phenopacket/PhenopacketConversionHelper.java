package uk.ac.ebi.biosamples.model.ga4gh.phenopacket;

import org.phenopackets.schema.v1.core.OntologyClass;
import org.phenopackets.schema.v1.core.PhenotypicFeature;
import org.phenopackets.schema.v1.core.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.service.OLSDataRetriever;

import java.util.Optional;

@Component
public class PhenopacketConversionHelper {
    private static final Logger LOG = LoggerFactory.getLogger(PhenopacketConversionHelper.class);

    public Optional<PhenopacketAttribute> convertAttribute(String type, String value, String ontologyId, String ontologyLabel) {
        return Optional.of(PhenopacketAttribute.build(type, value, ontologyId, ontologyLabel, false));
    }

    public Optional<PhenopacketAttribute> convertAttributeWithNegation(String type, String value, String ontologyId, String ontologyLabel) {
        return Optional.of(PhenopacketAttribute.build(type, value, ontologyId, ontologyLabel, true));
    }

    public Optional<PhenopacketAttribute> convertAttribute(String type, Attribute attribute) {
        Optional<PhenopacketAttribute> optionalPhenopacketAttribute = Optional.empty();
        if (!attribute.getIri().isEmpty()) {
            try {
                OLSDataRetriever retriever = new OLSDataRetriever();
                String firstIri = attribute.getIri().first();
                retriever.readOntologyJsonFromUrl(firstIri);
                optionalPhenopacketAttribute = Optional.of(
                        PhenopacketAttribute.build(type, attribute.getValue(),
                                retriever.getOntologyTermId(), retriever.getOntologyTermLabel()));
            } catch (Exception e) {
                LOG.warn("Failed to get IRI from OLS, possibly a wrong IRI format: {}", attribute.getIri().first(), e);
            }
        } else {
            optionalPhenopacketAttribute = Optional.of(
                    PhenopacketAttribute.build(type, attribute.getValue(), null, null));
        }

        return optionalPhenopacketAttribute;
    }

    public OntologyClass getOntology(PhenopacketAttribute attribute) {
        return OntologyClass.newBuilder()
                .setId(attribute.getOntologyId())
                .setLabel(attribute.getOntologyLabel())
                .build();
    }

    public PhenotypicFeature getPhenotype(PhenopacketAttribute attribute) {
        return PhenotypicFeature.newBuilder()
                .setType(getOntology(attribute))
                .setNegated(attribute.isNegate())
                .setDescription(attribute.getValue())
                .build();
    }

    public Optional<Resource> getResource(PhenopacketAttribute attribute) {
        Optional<Resource> optionalResource = Optional.empty();
        if (attribute.getOntologyId() != null) {
            String ontology = attribute.getOntologyId().split(":")[0];
            OLSDataRetriever retriever = new OLSDataRetriever();
            retriever.readResourceInfoFromUrl(ontology);

            optionalResource = Optional.of(Resource.newBuilder()
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
