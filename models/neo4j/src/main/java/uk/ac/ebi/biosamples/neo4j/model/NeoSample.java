package uk.ac.ebi.biosamples.neo4j.model;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.ArrayList;
import java.util.List;

public class NeoSample {
    private String accession;
    private String name;
    private String organism;
    private String taxId;
    private String sex;
    private String cellType;
    private String material;
    private String project;
    private String cellLine;
    private String organismPart;

    private List<NeoRelationship> relationships;
    private List<NeoExternalEntity> externalRefs;

    private NeoSample(String accession) {
        this.accession = accession;
        this.relationships = new ArrayList<>();
        this.externalRefs = new ArrayList<>();
    }

    public String getAccession() {
        return accession;
    }

    public String getName() {
        return name;
    }

    public String getOrganism() {
        return organism;
    }

    public String getTaxId() {
        return taxId;
    }

    public String getSex() {
        return sex;
    }

    public String getCellType() {
        return cellType;
    }

    public String getMaterial() {
        return material;
    }

    public String getProject() {
        return project;
    }

    public String getCellLine() {
        return cellLine;
    }

    public String getOrganismPart() {
        return organismPart;
    }

    public List<NeoRelationship> getRelationships() {
        return relationships;
    }

    public List<NeoExternalEntity> getExternalRefs() {
        return externalRefs;
    }

    public static NeoSample build(Sample sample) {
        NeoSample neoSample = new NeoSample(sample.getAccession());
        neoSample.name = sample.getName();
        neoSample.taxId = String.valueOf(sample.getTaxId());

        for (Attribute attribute : sample.getAttributes()) {
            if ("organism".equalsIgnoreCase(attribute.getType())) {
                neoSample.organism = attribute.getValue().toLowerCase();
            } else if ("sex".equalsIgnoreCase(attribute.getType())) {
                neoSample.sex = attribute.getValue().toLowerCase();
            } else if ("cellType".equalsIgnoreCase(attribute.getType().replaceAll("\\s+", ""))) {
                neoSample.cellType = attribute.getValue().toLowerCase();
            } else if ("material".equalsIgnoreCase(attribute.getType())) {
                neoSample.material = attribute.getValue().toLowerCase();
            } else if ("project".equalsIgnoreCase(attribute.getType())) {
                neoSample.project = attribute.getValue().toLowerCase();
            } else if ("cellLine".equalsIgnoreCase(attribute.getType().replaceAll("\\s+", ""))) {
                neoSample.cellLine = attribute.getValue().toLowerCase();
            } else if ("organismPart".equalsIgnoreCase(attribute.getType().replaceAll("\\s+", ""))) {
                neoSample.organismPart = attribute.getValue().toLowerCase();
            }
        }

        for (Relationship relationship : sample.getRelationships()) {
            if (relationship.getSource().equals(neoSample.accession)) {
                neoSample.relationships.add(NeoRelationship.build(relationship));
            }
        }

        for (ExternalReference ref : sample.getExternalReferences()) {
            neoSample.externalRefs.add(NeoExternalEntity.build(ref));
        }

        return neoSample;
    }

    public static NeoSample build(String accession) {
        return new NeoSample(accession);
    }
}
