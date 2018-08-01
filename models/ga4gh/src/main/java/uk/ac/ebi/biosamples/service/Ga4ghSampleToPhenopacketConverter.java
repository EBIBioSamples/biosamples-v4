package uk.ac.ebi.biosamples.service;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.Timestamps;
import org.phenopackets.schema.v1.PhenoPacket;
import org.phenopackets.schema.v1.core.Age;
import org.phenopackets.schema.v1.core.*;
import org.phenopackets.schema.v1.core.Individual;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.ga4gh.*;

import java.text.ParseException;
import java.util.*;

/**
 * GA4GH format to Phenopackets format data converter
 *
 * @author Dilshat Salikhov
 * @see "https://github.com/phenopackets/phenopacket-schema"
 */
@Service
public class Ga4ghSampleToPhenopacketConverter implements Converter<Ga4ghSample, PhenoPacket> {
    private SampleToGa4ghSampleConverter mapper;
    private OLSDataRetriever olsApiretreiver;
    private final ImmutableList<String> stopList = ImmutableList.of("treatment", "isolate");

    @Autowired
    public Ga4ghSampleToPhenopacketConverter(SampleToGa4ghSampleConverter mapper,
                                             OLSDataRetriever olsRetriever) {
        this.mapper = mapper;
        this.olsApiretreiver = olsRetriever;

    }

    /**
     * Generates phenopacket json string from ga4gh sample object
     * @param ga4ghSample
     * @return phenopacket encoded in json string
     */
    public String getJsonFormattedPhenopacket(Ga4ghSample ga4ghSample) {
        PhenoPacket phenoPacket = convert(ga4ghSample);
        try {
            return JsonFormat.printer().print(phenoPacket);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return "Invalid Protobuffer. Impossible to create phenopacket";
    }

    /**
     * Generates phenopacket json string from sample(Biosamples)
     * @param sample
     * @return phenopacket encoded in json string
     */
    public String getJsonFormattedPhenopacketFromSample(Sample sample) {
        Ga4ghSample ga4ghSample = mapper.convert(sample);
        return getJsonFormattedPhenopacket(ga4ghSample);
    }

    @Override
    public PhenoPacket convert(Ga4ghSample ga4ghSample) {
        PhenoPacket.Builder phenoPacket = PhenoPacket.newBuilder();
        phenoPacket.addIndividuals(mapIndividual(ga4ghSample));
        phenoPacket.addBiosamples(mapBiosample(ga4ghSample));
        Disease disease = mapDisease(ga4ghSample.getBio_characteristic(), ga4ghSample.getAttributes());
        if (disease != null) {
            phenoPacket.addDiseases(disease);
        }
        phenoPacket.setMetaData(createMetaData(ga4ghSample.getBio_characteristic()));
        return phenoPacket.build();
    }

    private Individual mapIndividual(Ga4ghSample ga4ghSample) {
        Individual.Builder individualBuilder = Individual.newBuilder();
        individualBuilder.setId(ga4ghSample.getId() + "-individual");
        OntologyClass.Builder ontologyBuilder = OntologyClass.newBuilder();
        Ga4ghOntologyTerm sex = null;
        Ga4ghOntologyTerm organism = null;
        for (Ga4ghBiocharacteristics term : ga4ghSample.getBio_characteristic()) {
            if (term.getDescription().contains("sex")) {
                sex = term.getOntology_terms().first(); //in sex field in all cases will be only one ontology term
            }
            if (term.getDescription().equals("organism") || term.getDescription().equals("Organism")) {
                organism = term.getOntology_terms().first(); //in organism field in all cases will be only one ontology term
            }
        }
        if (sex != null) {
            ontologyBuilder.setId(sex.getTerm_id());
            ontologyBuilder.setLabel(sex.getTerm_label());
        }
        individualBuilder.setSex(ontologyBuilder.build());
        if (organism != null) {
            individualBuilder.setTaxonomy(mapOntologyTerm(organism));
        }
        //TODO convert date of birth


        return individualBuilder.build();

    }


    private Biosample mapBiosample(Ga4ghSample ga4GhGa4ghSample) {
        Biosample.Builder biosampleBuilder = Biosample.newBuilder();
        biosampleBuilder.setId(ga4GhGa4ghSample.getId());


        try {
            biosampleBuilder.setCreated(Timestamps.parse(ga4GhGa4ghSample.getReleasedDate()));
            biosampleBuilder.setUpdated(Timestamps.parse(ga4GhGa4ghSample.getUpdatedDate()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (ga4GhGa4ghSample.getDataset_id() != null) {
            biosampleBuilder.setDatasetId(ga4GhGa4ghSample.getDataset_id());
        }
        if (ga4GhGa4ghSample.getDescription() != null) {
            biosampleBuilder.setDescription(ga4GhGa4ghSample.getDescription());
        }
        Ga4ghOntologyTerm organism = null;
        for (Ga4ghBiocharacteristics term : ga4GhGa4ghSample.getBio_characteristic()) {
            if (term.getDescription().equals("organism") || term.getDescription().equals("Organism")) {
                organism = term.getOntology_terms().first(); //in organism field in all cases will be only one ontology term
            }
        }
        if (organism != null) {
            biosampleBuilder.setTaxonomy(mapOntologyTerm(organism));
        }
        Ga4ghAge age = ga4GhGa4ghSample.getIndividual_age_at_collection();
        Age.Builder phenopacketAgeBuilder = Age.newBuilder();
        if (age != null) {
            phenopacketAgeBuilder.setAge(age.getAge());
            if (age.getAge_class() != null) {
                phenopacketAgeBuilder.setAgeClass(mapOntologyTerm(age.getAge_class()));
            }
        }
        biosampleBuilder.setIndividualAgeAtCollection(phenopacketAgeBuilder.build());
        biosampleBuilder.setIndividualId(ga4GhGa4ghSample.getId() + "-individual");
        if (ga4GhGa4ghSample.getName() != null) {
            biosampleBuilder.setName(ga4GhGa4ghSample.getName());
        }
        biosampleBuilder.addAllPhenotypes(mapBiocharacteristics(ga4GhGa4ghSample.getBio_characteristic()));
        return biosampleBuilder.build();
    }

    private Iterable<Phenotype> mapBiocharacteristics(SortedSet<Ga4ghBiocharacteristics> biocharacteristics) {
        Iterable<Phenotype> phenotypes = new ArrayList<>();
        for (Ga4ghBiocharacteristics biocharacteristic : biocharacteristics) {
            if (isBiocharacteristicRelatedToPhenotype(biocharacteristic) && !stopList.contains(biocharacteristic.getDescription())) {
                for (Ga4ghOntologyTerm characteristic : biocharacteristic.getOntology_terms()) {
                    Phenotype.Builder phenotypeBuilder = Phenotype.newBuilder();
                    OntologyClass.Builder typeBuilder = OntologyClass.newBuilder();
                    typeBuilder.setId(characteristic.getTerm_id());
                    typeBuilder.setLabel(characteristic.getTerm_label());
                    phenotypeBuilder.setType(typeBuilder.build());
                    phenotypeBuilder.setNegated(false); //biosamples doesnt contain negated ontology terms
                    ((ArrayList<Phenotype>) phenotypes).add(phenotypeBuilder.build());
                }
            }
        }


        return phenotypes;
    }

    private MetaData createMetaData(SortedSet<Ga4ghBiocharacteristics> biocharacteristics) {
        MetaData.Builder builder = MetaData.newBuilder();
        builder.setCreated(Timestamps.fromMillis(System.currentTimeMillis()));
        builder.setCreatedBy("Biosamples phenopacket exporter");
        Set<String> uniqueIds = new TreeSet<>();
        for (Ga4ghBiocharacteristics biocharacteristic : biocharacteristics) {
            if (!stopList.contains(biocharacteristic.getDescription())) {
                for (Ga4ghOntologyTerm term : biocharacteristic.getOntology_terms()) {
                    String id = term.getTerm_id().split(":")[0]; //term id presented by ontologyID:termId
                    if (!uniqueIds.contains(id)) {
                        builder.addResources(mapResource(id));
                        uniqueIds.add(id);
                    }
                }
            }
        }
        return builder.build();
    }


    private Disease mapDisease(SortedSet<Ga4ghBiocharacteristics> biocharacteristics, Ga4ghAttributes attributes) {
        Disease.Builder diseaseBuilder = Disease.newBuilder();
        Disease resultDisease = null;
        for (Ga4ghBiocharacteristics biocharacteristic : biocharacteristics) {
            String description = biocharacteristic.getDescription();
            if (description.equals("disease") || description.equals("Disease") || description.equals("disease state")) {
                Ga4ghOntologyTerm term = biocharacteristic.getOntology_terms().first();
                diseaseBuilder.setId(term.getTerm_id());
                diseaseBuilder.setLabel(term.getTerm_label());
                resultDisease = diseaseBuilder.build();
            }
        }
        if (resultDisease == null) {
            List<AttributeValue> attributeValues = attributes.getAttributes().get("disease");
            if (attributeValues == null) {
                attributeValues = attributes.getAttributes().get("disease state");
            }
            if (attributeValues == null) {
                attributeValues = attributes.getAttributes().get("Disease");
            }
            if (attributeValues != null) {
                AttributeValue disease = attributeValues.get(0);
                diseaseBuilder.setLabel((String) disease.getValue()); //disease attribute in biosamples in this case will be definetely without ontology term and will contain only one value with name of disease
                resultDisease = diseaseBuilder.build();
            }
        }

        return resultDisease;
    }


    private OntologyClass mapOntologyTerm(Ga4ghOntologyTerm term) {
        OntologyClass.Builder ontologyBuilder = OntologyClass.newBuilder();
        ontologyBuilder.setId(term.getTerm_id());
        ontologyBuilder.setLabel(term.getTerm_label());
        return ontologyBuilder.build();
    }


    /**
     * Build a @link{Resource} object querying the OLS api to expand an ontology iri
     * @param id the iri of the ontology
     * @return the expanded ontology Resource
     */
    private Resource mapResource(String id) {
        Resource.Builder resourceBuilder = Resource.newBuilder();
        olsApiretreiver.readResourceInfoFromUrl(id);
        resourceBuilder.setId(olsApiretreiver.getResourceId());
        resourceBuilder.setName(olsApiretreiver.getResourceName());
        resourceBuilder.setUrl(olsApiretreiver.getResourceUrl());
        resourceBuilder.setNamespacePrefix(olsApiretreiver.getResourcePrefix());
        resourceBuilder.setVersion(olsApiretreiver.getResourceVersion());
        return resourceBuilder.build();
    }


    private boolean isBiocharacteristicRelatedToPhenotype(Ga4ghBiocharacteristics biocharacteristic) {
        String description = biocharacteristic.getDescription();
        return !(description.contains("sex") ||
                description.equals("organism") ||
                description.equals("Organism") ||
                description.contains("disease") ||
                description.contains("Disease"));
    }

}

