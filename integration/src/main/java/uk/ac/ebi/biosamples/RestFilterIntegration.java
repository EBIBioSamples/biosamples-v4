package uk.ac.ebi.biosamples;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;

@Component
@Profile({"default", "rest", "test"})

public class RestFilterIntegration extends AbstractIntegration{

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private final IntegrationProperties integrationProperties;
    private final BioSamplesProperties bioSamplesProperties;

    public RestFilterIntegration(BioSamplesClient client, IntegrationProperties integrationProperties,
                                 BioSamplesProperties bioSamplesProperties) {
        super(client);
        this.integrationProperties = integrationProperties;
        this.bioSamplesProperties = bioSamplesProperties;
    }
    @Override
    protected void phaseOne() {
        Sample testSample1 = getTestSample1();
        Sample testSample2 = getTestSample2();
        Sample testSample3 = getTestSample3();
        if (client.fetchSampleResource(testSample1.getAccession()).isPresent()) {
            throw new RuntimeException("Test sample " + testSample1.getAccession() + " should not be available in phase one");
        } else {
            log.info("Persisting test sample 1");
            Resource<Sample> sampleReturned = client.persistSampleResource(testSample1);
            if (!sampleReturned.getContent().equals(testSample1)) {
                throw new RuntimeException("Persistent sample and submitted sample are not equal");
            }
        }


        if (client.fetchSampleResource(testSample2.getAccession()).isPresent()) {
            throw new RuntimeException("Test sample " + testSample2.getAccession() + " should not be available in phase one");
        } else {
            log.info("Persisting test sample 2");
            Resource<Sample> sampleReturned = client.persistSampleResource(testSample2);
            if (!sampleReturned.getContent().equals(testSample2)) {
                throw new RuntimeException("Persistent sample and submitted sample are not equal");
            }
        }

        if (client.fetchSampleResource(testSample3.getAccession()).isPresent()) {
            throw new RuntimeException("Test sample " + testSample3.getAccession() + " should not be available in phase one");
        } else {
            log.info("Persisting test sample 3");
            Resource<Sample> sampleReturned = client.persistSampleResource(testSample3);
            if (!sampleReturned.getContent().equals(testSample3)) {
                throw new RuntimeException("Persistent sample and submitted sample are not equal");
            }
        }
    }

    @Override
    protected void phaseTwo() {
        log.info("Getting sample 1 using filter on attribute");
        Sample testSample1 = getTestSample1();
        Filter attributeFilter = FilterBuilder.create().onAttribute("TestAttribute").withValue("FilterMe").build();
        PagedResources<Resource<Sample>> samplePage = client.fetchPagedSampleResource("",
                Collections.singletonList(attributeFilter),
                0, 10);
        if (samplePage.getMetadata().getTotalElements() != 1) {
            throw new RuntimeException("Unexpected number of results for attribute filter query: " + samplePage.getMetadata().getTotalElements());
        }
        Resource<Sample> restSample = samplePage.getContent().iterator().next();
        if (!restSample.getContent().equals(testSample1)) {
            throw new RuntimeException("Unexpected number of results for attribute filter query: " + samplePage.getMetadata().getTotalElements());
        }


        log.info("Getting sample 2 using filter on attribute");
        Sample testSample2 = getTestSample2();
        attributeFilter = FilterBuilder.create().onAttribute("testAttribute").withValue("filterMe").build();
        samplePage = client.fetchPagedSampleResource("",
                Collections.singletonList(attributeFilter),
                0, 10);
        if (samplePage.getMetadata().getTotalElements() != 1) {
            throw new RuntimeException("Unexpected number of results for attribute filter query: " + samplePage.getMetadata().getTotalElements());
        }
        restSample = samplePage.getContent().iterator().next();
        if (!restSample.getContent().getAccession().equals(testSample2.getAccession())) {
            throw new RuntimeException("Unexpected number of results for attribute filter query: " + samplePage.getMetadata().getTotalElements());
        }


        log.info("Getting sample 2 using filter on name");
        Filter nameFilter = FilterBuilder.create().onName(testSample2.getName()).build();
        samplePage = client.fetchPagedSampleResource("",
                Collections.singletonList(nameFilter),
                0, 10);
        if (samplePage.getMetadata().getTotalElements() != 1) {
            throw new RuntimeException("Unexpected number of results for attribute filter query: " + samplePage.getMetadata().getTotalElements());
        }
        restSample = samplePage.getContent().iterator().next();
        if (!restSample.getContent().getAccession().equals(testSample2.getAccession())) {
            throw new RuntimeException("Unexpected number of results for attribute filter query: " + samplePage.getMetadata().getTotalElements());
        }

        log.info("Getting sample 1 and 2 using filter on accession");
        Filter accessionFilter = FilterBuilder.create().onAccession("TestFilter[12]").build();
        samplePage = client.fetchPagedSampleResource("",
                Collections.singletonList(accessionFilter),
                0, 10);
        if (samplePage.getMetadata().getTotalElements() != 2) {
            throw new RuntimeException("Unexpected number of results for attribute filter query: " + samplePage.getMetadata().getTotalElements());
        }

        if (!samplePage.getContent().stream().allMatch(r-> r.getContent().getAccession().equals(testSample1.getAccession()) || r.getContent().getAccession().equals(testSample2.getAccession()))) {
            throw new RuntimeException("Unexpected number of results for attribute filter query: " + samplePage.getMetadata().getTotalElements());
        }

    }

    @Override
    protected void phaseThree() {
        log.info("Getting sample 1 using filter on date range");
        Sample testSample1 = getTestSample1();
        Filter dateFilter = FilterBuilder.create().onReleaseDate()
        		.from(testSample1.getRelease().minusSeconds(2))
        		.until(testSample1.getRelease().plusSeconds(2))
        		.build();
        PagedResources<Resource<Sample>> samplePage = client.fetchPagedSampleResource("",
                Collections.singletonList(dateFilter),
                0, 10);
        if (samplePage.getMetadata().getTotalElements() < 1) {
            throw new RuntimeException("Unexpected number of results for date range filter query: " + samplePage.getMetadata().getTotalElements());
        }
        boolean match = samplePage.getContent().stream().anyMatch(resource -> resource.getContent().getAccession().equals(testSample1.getAccession()));
        if (!match) {
            throw new RuntimeException("Returned sample doesn't match the expected sample " + testSample1.getAccession());
        }

        log.info("Getting sample 3 using filter on relation");
        Sample testSample3 = getTestSample3();

        Filter relFilter = FilterBuilder.create().onRelation("parent of").withValue(getTestSample2().getAccession()).build();
        samplePage = client.fetchPagedSampleResource("", Collections.singletonList(relFilter), 0, 10);

        if (samplePage.getMetadata().getTotalElements() < 1) {
            throw new RuntimeException("Unexpected number of results for relation filter query: " + samplePage.getMetadata().getTotalElements());
        }
        match = samplePage.getContent().stream().anyMatch(resource -> resource.getContent().getAccession().equals(testSample3.getAccession()));
        if (!match) {
            throw new RuntimeException("Returned sample doesn't match the expected sample " + testSample3.getAccession());
        }


        log.info("Getting sample 2 using filter on inverse relation");
        Filter invRelFilter = FilterBuilder.create().onInverseRelation("parent of").withValue(getTestSample3().getAccession()).build();
        samplePage = client.fetchPagedSampleResource("", Collections.singletonList(invRelFilter), 0, 10);

        if (samplePage.getMetadata().getTotalElements() < 1) {
            throw new RuntimeException("Unexpected number of results for relation filter query: " + samplePage.getMetadata().getTotalElements());
        }
        match = samplePage.getContent().stream().anyMatch(resource -> resource.getContent().getAccession().equals(getTestSample2().getAccession()));
        if (!match) {
            throw new RuntimeException("Returned sample doesn't match the expected sample " + testSample3.getAccession());
        }
    }

    @Override
    protected void phaseFour() {
        log.info("Getting results filtered by domains");
//        Sample testSample1 = getTestSample1();
//        Sample testSample2 = getTestSample2();

        Filter domainFilter = FilterBuilder.create().onDomain("self.BiosampleIntegrationTest").build();
        PagedResources<Resource<Sample>> samplePage = client.fetchPagedSampleResource("",
                Collections.singletonList(domainFilter),
                0, 10);
        if (samplePage.getMetadata().getTotalElements() < 1) {
            throw new RuntimeException("Unexpected number of results for domain filter query: " + samplePage.getMetadata().getTotalElements());
        }

//        List<Resource<Sample>> samples = samplePage.getContent().stream().filter(resource -> {
//            String sampleAccession =resource.getContent().getAccession();
//            return sampleAccession.equalsIgnoreCase(testSample1.getAccession()) ||
//                    sampleAccession.equalsIgnoreCase(testSample2.getAccession());
//        }).collect(Collectors.toList());
//
//        if (samples.size() != 2) {
//            throw new RuntimeException("Results contains a number of samples not matching the test samples");
//        }

    }

    @Override
    protected void phaseFive() {

    }

    public Sample getTestSample1() {
        String name = "Test Filter Sample 1";
        String accession = "TestFilter1";
        String domain = "self.BiosampleIntegrationTest";
        Instant update = Instant.parse("1999-12-25T11:36:57.00Z");
        Instant release = Instant.parse("1999-12-25T11:36:57.00Z");

        SortedSet<Attribute> attributes = new TreeSet<>();
        attributes.add(Attribute.build("TestAttribute", "FilterMe"));

        return Sample.build(name, accession, domain, release, update, attributes, new TreeSet<>(), new TreeSet<>(), null, null, null);
    }

    public Sample getTestSample2() {
        String name = "Test Filter Sample 2";
        String accession = "TestFilter2";
        String domain = "self.BiosampleIntegrationTest";
        Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
        Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

        SortedSet<Attribute> attributes = new TreeSet<>();
        attributes.add(
                Attribute.build("testAttribute", "filterMe", "http://www.ebi.ac.uk/efo/EFO_0001071", null));

        return Sample.build(name, accession, domain, release, update, attributes, new TreeSet<>(), new TreeSet<>(), null, null, null);
    }

    public Sample getTestSample3() {
        String name = "Test Filter Sample 3";
        String accession = "TestFilter3";
        String domain = "self.BiosampleIntegrationTest";
        Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
        Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

        SortedSet<Relationship> relations = new TreeSet<>();
        relations.add(Relationship.build(accession, "parent of", "TestFilter2"));


        return Sample.build(name, accession, domain, release, update, null, relations, new TreeSet<>(), null, null, null);
    }

}
