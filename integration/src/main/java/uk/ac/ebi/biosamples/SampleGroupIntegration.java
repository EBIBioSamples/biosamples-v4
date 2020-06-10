package uk.ac.ebi.biosamples;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

@Component
public class SampleGroupIntegration extends AbstractIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleGroupIntegration.class);
    private final RestTemplate restTemplate;
    private BioSamplesProperties clientProperties;
    private final BioSamplesClient annonymousClient;


    public SampleGroupIntegration(BioSamplesClient client, RestTemplateBuilder restTemplateBuilder, BioSamplesProperties clientProperties) {
        super(client);
        this.restTemplate = restTemplateBuilder.build();
        this.clientProperties = clientProperties;
        this.annonymousClient = new BioSamplesClient(this.clientProperties.getBiosamplesClientUri(), restTemplateBuilder, null, null, clientProperties);
    }

    @Override
    protected void phaseOne() {
        Sample testSampleGroup = getSampleTest1();
        Sample testSample = getSampleTest2();
        Optional<Sample> optionalSample = fetchUniqueSampleByName(testSampleGroup.getName());

        if (optionalSample.isPresent()) {
            throw new IntegrationTestFailException("RestIntegration test sample should not be available during phase 1", Phase.ONE);
        } else {
            try {
                Resource<Sample> groupResource = client.persistSampleGroup(testSampleGroup, null).get();
                Resource<Sample> sampleResource = client.persistSampleResource(testSample);
                Sample testSampleWithAccession = Sample.Builder.fromSample(testSampleGroup)
                        .withAccession(groupResource.getContent().getAccession())
                        .build();

                if (!testSampleWithAccession.equals(groupResource.getContent())) {
                    LOGGER.error("Expected response (" + groupResource.getContent() + ") to equal submission (" + testSampleGroup + ")");
                    throw new IntegrationTestFailException("Expected response (" + groupResource.getContent() + ") to equal submission (" + testSampleGroup + ")");
                }

                if (sampleResource.getContent().getAccession() == null) {
                    throw new IntegrationTestFailException("Failed to persist sample: " + testSample.getName());
                }
            } catch (Exception e) {
                throw new IntegrationTestFailException(e.toString());
            }
        }
    }

    @Override
    protected void phaseTwo() {
        Sample testSampleGroup = getSampleTest1();
        Optional<Sample> optionalGroupSample = fetchUniqueSampleByName(testSampleGroup.getName());
        if (optionalGroupSample.isEmpty()) {
            throw new IntegrationTestFailException("RestIntegration test sample should not be available during phase 1", Phase.TWO);
        }

        Sample testSample = getSampleTest2();
        Optional<Sample> optionalSample = fetchUniqueSampleByName(testSample.getName());
        if (optionalSample.isEmpty()) {
            throw new IntegrationTestFailException("RestIntegration test sample should not be available during phase 1", Phase.TWO);
        }

        SortedSet<Relationship> relationships = new TreeSet<>();
        relationships.add(Relationship.build(optionalGroupSample.get().getAccession(), "Has member", optionalSample.get().getAccession()));
        testSampleGroup = Sample.Builder.fromSample(testSampleGroup).withRelationships(relationships)
                .withAccession(optionalGroupSample.get().getAccession()).build();

        try {
            Resource<Sample> resource = client.persistSampleGroup(testSampleGroup, null).get();
            if (resource.getContent().getAccession() == null) {
                LOGGER.error("Expected response (" + resource.getContent() + ") to equal submission (" + testSampleGroup + ")");
                throw new IntegrationTestFailException("Expected response (" + resource.getContent() + ") to equal submission (" + testSampleGroup + ")");
            }
        } catch (Exception e) {
            throw new IntegrationTestFailException("Integration test fail exception");
        }
    }

    @Override
    protected void phaseThree() {
        // nothing to do here
    }

    @Override
    protected void phaseFour() {
        // nothing to do here
    }

    @Override
    protected void phaseFive() {
        //nothing to do here
    }

    private Sample getSampleTest1() {
        String name = "SampleGroupIntegration_sampleGroup_1";
        Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

        SortedSet<Attribute> attributes = new TreeSet<>();
        attributes.add(
                Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
        attributes.add(Attribute.build("age", "3", null, Collections.emptyList(), "year"));
        attributes.add(Attribute.build("organism part", "lung"));
        attributes.add(Attribute.build("organism part", "heart"));
        attributes.add(Attribute.build("sex", "female", null, Sets.newHashSet("http://purl.obolibrary.org/obo/PATO_0000383", "http://www.ebi.ac.uk/efo/EFO_0001265"), null));

        SortedSet<Relationship> relationships = new TreeSet<>();
        SortedSet<ExternalReference> externalReferences = new TreeSet<>();
        externalReferences.add(ExternalReference.build("http://www.google.com"));

        SortedSet<Organization> organizations = new TreeSet<>();
        organizations.add(new Organization.Builder()
                .name("Jo Bloggs Inc")
                .role("user")
                .email("help@jobloggs.com")
                .url("http://www.jobloggs.com")
                .build());

        SortedSet<Contact> contacts = new TreeSet<>();
        contacts.add(new Contact.Builder()
                .name("Joe Bloggs")
                .role("Submitter")
                .email("jobloggs@joblogs.com")
                .build());

        SortedSet<Publication> publications = new TreeSet<>();
        publications.add(new Publication.Builder()
                .doi("10.1093/nar/gkt1081")
                .pubmed_id("24265224")
                .build());

        return new Sample.Builder(name)
                .withRelease(release).withDomain(defaultIntegrationSubmissionDomain)
                .withAttributes(attributes)
                .withRelationships(relationships).withExternalReferences(externalReferences)
                .withOrganizations(organizations).withContacts(contacts).withPublications(publications)
                .build();
    }

    private Sample getSampleTest2() {
        String name = "SampleGroupIntegration_sample_1";
        Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

        SortedSet<Attribute> attributes = new TreeSet<>();
        attributes.add(
                Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
        attributes.add(Attribute.build("age", "3", null, Collections.emptyList(), "year"));
        attributes.add(Attribute.build("organism part", "lung"));
        attributes.add(Attribute.build("organism part", "heart"));
        attributes.add(Attribute.build("sex", "female", null, Sets.newHashSet("http://purl.obolibrary.org/obo/PATO_0000383", "http://www.ebi.ac.uk/efo/EFO_0001265"), null));

        SortedSet<Relationship> relationships = new TreeSet<>();
        SortedSet<ExternalReference> externalReferences = new TreeSet<>();
        externalReferences.add(ExternalReference.build("http://www.google.com"));

        SortedSet<Organization> organizations = new TreeSet<>();
        SortedSet<Contact> contacts = new TreeSet<>();
        SortedSet<Publication> publications = new TreeSet<>();

        return new Sample.Builder(name)
                .withRelease(release).withDomain(defaultIntegrationSubmissionDomain)
                .withAttributes(attributes)
                .withRelationships(relationships).withExternalReferences(externalReferences)
                .withOrganizations(organizations).withContacts(contacts).withPublications(publications)
                .build();
    }

    @PreDestroy
    public void destroy() {
        annonymousClient.close();
    }

}
