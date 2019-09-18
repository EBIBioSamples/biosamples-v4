package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;

import java.time.Instant;
import java.util.*;

@Component
public class RestStaticViewIntegration extends AbstractIntegration {

    private Logger log = LoggerFactory.getLogger(this.getClass());


    public RestStaticViewIntegration(BioSamplesClient client) {
        super(client);
    }

    @Override
    protected void phaseOne() {
        Sample test1 = getSampleTest1();
        Sample test2 = getSampleTest2();
        Sample test4 = getSampleTest4();
        Sample test5 = getSampleTest5();

        //put a private sample
        Resource<Sample> resource = client.persistSampleResource(test1);
        if (!test1.equals(resource.getContent())) {
            throw new RuntimeException("Expected response (" + resource.getContent() + ") to equal submission (" + test1 + ")");
        }

        //put a sample that refers to a non-existing sample
        resource = client.persistSampleResource(test2);
        if (!test2.equals(resource.getContent())) {
            throw new RuntimeException("Expected response (" + resource.getContent() + ") to equal submission (" + test2 + ")");
        }

        resource = client.persistSampleResource(test4);
        if (!test4.equals(resource.getContent())) {
            throw new RuntimeException("Expected response (" + resource.getContent() + ") to equal submission (" + test4 + ")");
        }

        Set<Attribute> attributesPre;
        Set<Attribute> attributesPost;

        attributesPre = new HashSet<>();
        attributesPre.add(Attribute.build("organism", "9606"));
        attributesPost = new HashSet<>();
        attributesPost.add(Attribute.build("organism", "Homo sapiens"));
        client.persistCuration(test2.getAccession(),
                Curation.build(attributesPre, attributesPost, null, null), "self.BiosampleIntegrationTest");


        attributesPre = new HashSet<>();
        attributesPre.add(Attribute.build("organism", "Homo sapiens"));
        attributesPost = new HashSet<>();
        attributesPost.add(Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
        client.persistCuration(test2.getAccession(),
                Curation.build(attributesPre, attributesPost, null, null), "self.BiosampleIntegrationTest");


        SortedSet<Relationship> relationships = new TreeSet<>();
        relationships.add(Relationship.build("SAMEA648208574", "derived from", getSampleTest2().getAccession()));
        relationships.add(Relationship.build("SAMEA648208574", "derive to", getSampleTest5().getAccession()));
        Sample sample4WithRelationships = Sample.Builder.fromSample(test4).withRelationships(relationships).build();
        resource = client.persistSampleResource(sample4WithRelationships);
        if (!sample4WithRelationships.equals(resource.getContent())) {
            throw new RuntimeException("Expected response (" + resource.getContent() + ") to equal submission (" + sample4WithRelationships + ")");
        }

        resource = client.persistSampleResource(test5);
        // Build inverse relationships for sample5
        SortedSet<Relationship> test5AllRelationships = test5.getRelationships();
        test5AllRelationships.add(Relationship.build(test4.getAccession(), "derive to", test5.getAccession()));
        test5 = Sample.Builder.fromSample(test5).withRelationships(test5AllRelationships).build();
        if (!test5.equals(resource.getContent())) {
            throw new RuntimeException("Expected response (" + resource.getContent() + ") to equal submission (" + test5 + ")");
        }

        relationships = new TreeSet<>();
        relationships.add(Relationship.build("SAMEA648208575", "derived from", getSampleTest2().getAccession()));
        Sample sample5WithRelationships = Sample.Builder.fromSample(test5).withRelationships(relationships).build();
        resource = client.persistSampleResource(sample5WithRelationships);

        relationships = new TreeSet<>();
        relationships.add(Relationship.build(test4.getAccession(), "derive to", test5.getAccession()));//inverse relationship
        relationships.add(Relationship.build("SAMEA648208575", "derived from", getSampleTest2().getAccession()));
        Sample sample5WithInverseRelationships = Sample.Builder.fromSample(sample5WithRelationships)
                .addRelationship(Relationship.build(test4.getAccession(), "derive to", test5.getAccession()))
                .build();

        if (!sample5WithInverseRelationships.equals(resource.getContent())) {
            throw new RuntimeException("Expected response (" + resource.getContent() + ") to equal submission (" + sample5WithRelationships + ")");
        }
    }

    @Override
    protected void phaseTwo() {
        Sample test1 = getSampleTest1();
        Sample test2 = getSampleTest2();
        Sample test4 = getSampleTest4();
        Sample test5 = getSampleTest4();

        List<Resource<Sample>> samples = new ArrayList<>();
        for (Resource<Sample> sample : client.fetchSampleResourceAll()) {
            samples.add(sample);
        }

        if (samples.size() <= 0) {
            throw new RuntimeException("No search results found!");
        }

        //check that the private sample is not in search results
        //check that the referenced non-existing sample not in search result
        for (Resource<Sample> resource : client.fetchSampleResourceAll()) {
            log.trace("" + resource);
            if (resource.getContent().getAccession().equals(test1.getAccession())) {
                throw new RuntimeException("Found non-public sample " + test1.getAccession() + " in search samples");
            }
        }

        testDynamicAndStaticView(test2.getAccession());
        testDynamicAndStaticView(test5.getAccession());
        testDynamicAndStaticView(test4.getAccession());

        //delete relationships again
        Resource<Sample> resource = client.persistSampleResource(test4);
        if (!test4.equals(resource.getContent())) {
            throw new RuntimeException("Expected response (" + resource.getContent() + ") to equal submission (" + test4 + ")");
        }
    }

    @Override
    protected void phaseThree() {
        Sample test4 = getSampleTest4();
        testDynamicAndStaticView(test4.getAccession());
    }

    @Override
    protected void phaseFour() {
    }


    @Override
    protected void phaseFive() {
    }

    private void testDynamicAndStaticView(String accession) {
        Sample dynamicSample = client.fetchSampleResource(accession, Optional.empty(), null, StaticViewWrapper.StaticView.SAMPLES_DYNAMIC).get().getContent();
        Sample staticSample = client.fetchSampleResource(accession, Optional.empty(), null, StaticViewWrapper.StaticView.SAMPLES_CURATED).get().getContent();
        Sample sample = client.fetchSampleResource(accession).get().getContent();

        if (!dynamicSample.equals(staticSample) || !staticSample.equals(sample)) {
            throw new RuntimeException("Expected response (" + dynamicSample + ") to equal submission (" + staticSample + ")");
        }

        if (!dynamicSample.equals(staticSample) || !sample.equals(sample)) {
            throw new RuntimeException("Expected response (" + dynamicSample + ") to equal submission (" + sample + ")");
        }
    }

    private Sample getSampleTest1() {
        String name = "Test Sample";
        String accession = "SAMEA648208571";
        String domain = "self.BiosampleIntegrationTest";
        Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
        Instant release = Instant.parse("2116-04-01T11:36:57.00Z");

        SortedSet<Attribute> attributes = new TreeSet<>();
        attributes.add(
                Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

//		return Sample.build(name, accession, "self.BiosampleIntegrationTest", release, update, attributes, new TreeSet<>(), new TreeSet<>(), null, null, null);
        return new Sample.Builder(name, accession).withDomain(domain).withRelease(release).withUpdate(update)
                .withAttributes(attributes).build();
    }

    private Sample getSampleTest2() {
        String name = "Test Sample the second";
        String accession = "SAMEA648208572";
        String domain = "self.BiosampleIntegrationTest";
        Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
        Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

        SortedSet<Attribute> attributes = new TreeSet<>();
        attributes.add(
                Attribute.build("organism", "9606"));

        SortedSet<Relationship> relationships = new TreeSet<>();
        relationships.add(Relationship.build("SAMEA648208572", "derived from", "SAMEA648208573"));

//		return Sample.build(name, accession, "self.BiosampleIntegrationTest", release, update, attributes, relationships, new TreeSet<>(), null, null, null);
        return new Sample.Builder(name, accession).withDomain(domain).withRelease(release).withUpdate(update)
                .withAttributes(attributes).build();
    }

    private Sample getSampleTest4() {
        String name = "Test Sample the fourth";
        String accession = "SAMEA648208574";
        String domain = "self.BiosampleIntegrationTest";
        Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
        Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

        SortedSet<Attribute> attributes = new TreeSet<>();
        attributes.add(
                Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

        // TODO need to add inverse relationships later
//        SortedSet<Relationship> relationships = new TreeSet<>();
//        relationships.add(Relationship.build("SAMEA648208574", "derived from", getSampleTest2().getAccession()));
//        relationships.add(Relationship.build("SAMEA648208574", "derive to", getSampleTest5().getAccession()));

//		return Sample.build(name, accession, "self.BiosampleIntegrationTest", release, update, attributes, relationships, new TreeSet<>(), null, null, null);
        return new Sample.Builder(name, accession).withDomain(domain).withRelease(release).withUpdate(update)
                .withAttributes(attributes)
//                .withRelationships(relationships)
                .build();
    }

    private Sample getSampleTest5() {
        String name = "Test Sample the fifth";
        String accession = "SAMEA648208575";
        String domain = "self.BiosampleIntegrationTest";
        Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
        Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

        SortedSet<Attribute> attributes = new TreeSet<>();
        attributes.add(
                Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

        // TODO need to add inverse relationships later
        SortedSet<Relationship> relationships = new TreeSet<>();

//		return Sample.build(name, accession, "self.BiosampleIntegrationTest", release, update, attributes, relationships, new TreeSet<>(), null, null, null);
        return new Sample.Builder(name, accession).withDomain(domain).withRelease(release).withUpdate(update)
                .withAttributes(attributes).build();
    }

}
