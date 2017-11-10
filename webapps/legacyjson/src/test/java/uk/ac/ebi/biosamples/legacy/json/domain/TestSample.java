package uk.ac.ebi.biosamples.legacy.json.domain;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

import java.time.Instant;
import java.util.SortedSet;
import java.util.TreeSet;

public class TestSample {

    private final String accession;
    private String name;
    private Instant releaseDate;
    private SortedSet<Attribute> attributes;
    private SortedSet<Relationship> relationships;
    private final String testDomain = "testDomain";


    public TestSample(String accession) {
        this.accession = accession;
        this.name = "test";
        this.attributes = new TreeSet<>();
        this.relationships = new TreeSet<>();
        this.releaseDate = Instant.now();
    }

    public TestSample withName(String name) {
        this.name = name;
        return this;
    }

    public TestSample withAttribute(Attribute attribute) {
        this.attributes.add(attribute);
        return this;
    }

    public TestSample withRelationship(Relationship rel) {
        this.relationships.add(rel);
        return this;
    }

    public TestSample releasedOn(Instant releaseDate) {
        this.releaseDate = releaseDate;
        return this;
    }

    public Sample build() {
        return Sample.build(this.name,
                this.accession,
                this.testDomain,
                this.releaseDate,
                Instant.now(),
                this.attributes,
                this.relationships,
                null);
    }
}
