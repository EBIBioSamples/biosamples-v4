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
    private Integer taxId;
    private String sex;

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

    public Integer getTaxId() {
        return taxId;
    }

    public String getSex() {
        return sex;
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
        neoSample.taxId = sample.getTaxId();

        for (Attribute attribute : sample.getAttributes()) {
            if ("organism".equalsIgnoreCase(attribute.getType())) {
                neoSample.organism = attribute.getValue();
            } else if ("sex".equalsIgnoreCase(attribute.getType())) {
                neoSample.sex = attribute.getValue();
            }
        }

        for (Relationship relationship : sample.getRelationships()) {
            if (relationship.getSource().equals(neoSample.accession)) {
                neoSample.relationships.add(NeoRelationship.build(relationship));
            }
        }

        for (ExternalReference ref : sample.getExternalReferences()) {
            neoSample.externalRefs.add(NeoExternalEntity.build(ref.getUrl(), "ENA", ""));
        }

        return neoSample;
    }

    public static NeoSample build(String accession) {
        return new NeoSample(accession);
    }
}
