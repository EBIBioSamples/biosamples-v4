package uk.ac.ebi.biosamples.service.phenopackets_exportation_service;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.*;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.Timestamps;
import org.phenopackets.schema.v1.PhenoPacket;
import org.phenopackets.schema.v1.core.*;
import org.phenopackets.schema.v1.core.Age;
import org.phenopackets.schema.v1.core.Biosample;
import org.phenopackets.schema.v1.core.Individual;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.ga4gh_model.*;
import uk.ac.ebi.biosamples.service.ga4ghService.BiosampleToGA4GHMapper;
import uk.ac.ebi.biosamples.service.ga4ghService.BiosamplesRetriever;
import uk.ac.ebi.biosamples.service.ga4ghService.OLSDataRetriever;

import java.text.ParseException;
import java.util.*;

@Service
public class BiosampleToPhenopacketExporter {
    private BiosamplesRetriever retriever;
    private BiosampleToGA4GHMapper mapper;
    private OLSDataRetriever olsApiretreiver;
    private final ImmutableList<String> stopList = ImmutableList.of("treatment", "isolate");

    @Autowired
    public BiosampleToPhenopacketExporter(BiosamplesRetriever retriever,
                                          BiosampleToGA4GHMapper mapper,
                                          OLSDataRetriever olsRetriever) {
        this.retriever = retriever;
        this.mapper = mapper;
        this.olsApiretreiver = olsRetriever;

    }

    public PhenoPacket map(uk.ac.ebi.biosamples.model.ga4gh_model.Biosample biosample) {
        PhenoPacket.Builder phenoPacket = PhenoPacket.newBuilder();
        phenoPacket.addIndividuals(mapIndividual(biosample));
        phenoPacket.addBiosamples(mapBiosample(biosample));
        Disease disease = mapDisease(biosample.getBio_characteristic(), biosample.getAttributes());
        if (disease != null) {
            phenoPacket.addDiseases(disease);
        }
        phenoPacket.setMetaData(createMetaData(biosample.getBio_characteristic()));
        return phenoPacket.build();
    }

    private Individual mapIndividual(uk.ac.ebi.biosamples.model.ga4gh_model.Biosample biosample) {
        Individual.Builder individualBuilder = Individual.newBuilder();
        individualBuilder.setId(biosample.getId() + "-individual");
        OntologyClass.Builder ontologyBuilder = OntologyClass.newBuilder();
        OntologyTerm sex = null;
        OntologyTerm organism = null;
        for (Biocharacteristics term : biosample.getBio_characteristic()) {
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
        if(organism!=null) {
            individualBuilder.setTaxonomy(mapOntologyTerm(organism));
        }
        //TODO map date of birth


        return individualBuilder.build();

    }

    public String getJsonFormattedPhenopacket(uk.ac.ebi.biosamples.model.ga4gh_model.Biosample biosample) {
        PhenoPacket phenoPacket = map(biosample);
        try {
            return JsonFormat.printer().print(phenoPacket);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return "Invalid Protobuffer. Impossible to create phenopacket";
    }

    public String getJsonFormattedPhenopacketByAccession(String accession) {
        Sample sample = retriever.getSampleById(accession);
        uk.ac.ebi.biosamples.model.ga4gh_model.Biosample biosample = mapper.mapSampleToGA4GH(sample);
        return getJsonFormattedPhenopacket(biosample);
    }

    public String getJsonFormattedPhenopacketFromSample(Sample sample) {
        uk.ac.ebi.biosamples.model.ga4gh_model.Biosample biosample = mapper.mapSampleToGA4GH(sample);
        return getJsonFormattedPhenopacket(biosample);
    }

    private Biosample mapBiosample(uk.ac.ebi.biosamples.model.ga4gh_model.Biosample ga4ghBiosample) {
        Biosample.Builder biosampleBuilder = Biosample.newBuilder();
        biosampleBuilder.setId(ga4ghBiosample.getId());


        try {
            biosampleBuilder.setCreated(Timestamps.parse(ga4ghBiosample.getReleasedDate()));
            biosampleBuilder.setUpdated(Timestamps.parse(ga4ghBiosample.getUpdatedDate()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (ga4ghBiosample.getDataset_id() != null) {
            biosampleBuilder.setDatasetId(ga4ghBiosample.getDataset_id());
        }
        if (ga4ghBiosample.getDescription() != null) {
            biosampleBuilder.setDescription(ga4ghBiosample.getDescription());
        }
        OntologyTerm organism = null;
        for (Biocharacteristics term : ga4ghBiosample.getBio_characteristic()) {
            if (term.getDescription().equals("organism") || term.getDescription().equals("Organism")) {
                organism = term.getOntology_terms().first(); //in organism field in all cases will be only one ontology term
            }
        }
        if(organism!=null) {
            biosampleBuilder.setTaxonomy(mapOntologyTerm(organism));
        }
        uk.ac.ebi.biosamples.model.ga4gh_model.Age age = ga4ghBiosample.getIndividual_age_at_collection();
        Age.Builder phenopacketAgeBuilder = Age.newBuilder();
        if (age != null) {
            phenopacketAgeBuilder.setAge(age.getAge());
            if (age.getAge_class() != null) {
                phenopacketAgeBuilder.setAgeClass(mapOntologyTerm(age.getAge_class()));
            }
        }
        biosampleBuilder.setIndividualAgeAtCollection(phenopacketAgeBuilder.build());
        biosampleBuilder.setIndividualId(ga4ghBiosample.getId() + "-individual");
        if (ga4ghBiosample.getName() != null) {
            biosampleBuilder.setName(ga4ghBiosample.getName());
        }
        biosampleBuilder.addAllPhenotypes(mapBiocharacteristics(ga4ghBiosample.getBio_characteristic()));
        return biosampleBuilder.build();
    }


    private Iterable<Phenotype> mapBiocharacteristics(SortedSet<Biocharacteristics> biocharacteristics) {
        Iterable<Phenotype> phenotypes = new ArrayList<>();
        for (Biocharacteristics biocharacteristic : biocharacteristics) {
            if (isBiocharacteristicRelatedToPhenotype(biocharacteristic) && !stopList.contains(biocharacteristic.getDescription())) {
                for (OntologyTerm characteristic : biocharacteristic.getOntology_terms()) {
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

    private MetaData createMetaData(SortedSet<Biocharacteristics> biocharacteristics) {
        MetaData.Builder builder = MetaData.newBuilder();
        builder.setCreated(Timestamps.fromMillis(System.currentTimeMillis()));
        builder.setCreatedBy("Biosamples phenopacket exporter");
        Set<String> uniqueIds = new TreeSet<>();
        for (Biocharacteristics biocharacteristic : biocharacteristics) {
            if (!stopList.contains(biocharacteristic.getDescription())) {
                for (OntologyTerm term : biocharacteristic.getOntology_terms()) {
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

    private Disease mapDisease(SortedSet<Biocharacteristics> biocharacteristics, Attributes attributes) {
        Disease.Builder diseaseBuilder = Disease.newBuilder();
        Disease resultDisease = null;
        for (Biocharacteristics biocharacteristic : biocharacteristics) {
            String description = biocharacteristic.getDescription();
            if (description.equals("disease") || description.equals("Disease") || description.equals("disease state") ) {
                OntologyTerm term = biocharacteristic.getOntology_terms().first();
                diseaseBuilder.setId(term.getTerm_id());
                diseaseBuilder.setLabel(term.getTerm_label());
                resultDisease = diseaseBuilder.build();
            }
        }
        if (resultDisease == null) {
            List<AttributeValue> attributeValues = attributes.getAttributes().get("disease");
            if(attributeValues==null){
                attributeValues = attributes.getAttributes().get("disease state");
            }
            if(attributeValues==null){
                attributeValues = attributes.getAttributes().get("Disease");
            }
            if(attributeValues!=null) {
                AttributeValue disease = attributeValues.get(0);
                diseaseBuilder.setLabel((String) disease.getValue()); //disease attribute in biosamples in this case will be definetely without ontology term and will contain only one value with name of disease
                resultDisease = diseaseBuilder.build();
            }
        }

        return resultDisease;
    }

    private OntologyClass mapOntologyTerm(OntologyTerm term) {
        OntologyClass.Builder ontologyBuilder = OntologyClass.newBuilder();
        ontologyBuilder.setId(term.getTerm_id());
        ontologyBuilder.setLabel(term.getTerm_label());
        return ontologyBuilder.build();
    }

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

    private boolean isBiocharacteristicRelatedToPhenotype(Biocharacteristics biocharacteristic) {
        String description = biocharacteristic.getDescription();
        return !(description.contains("sex") ||
                description.equals("organism") ||
                description.equals("Organism") ||
                description.contains("disease") ||
                description.contains("Disease"));
    }

}

