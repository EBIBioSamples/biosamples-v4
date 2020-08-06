package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

import java.time.Instant;
import java.util.*;

@Component
@Order(6)
//@Profile({ "default", "rest" })
public class RestExternalReferenceIntegration extends AbstractIntegration {
    public RestExternalReferenceIntegration(BioSamplesClient client) {
        super(client);
    }

    @Override
    protected void phaseOne() {
        Sample sample = getSampleTest1();
        client.persistSampleResource(sample);
    }

    @Override
    protected void phaseTwo() {
        Sample sample = getSampleTest1();
        Optional<Sample> optionalSample = fetchUniqueSampleByName(sample.getName());
        if (optionalSample.isPresent()) {
            sample = Sample.Builder.fromSample(sample).withAccession(optionalSample.get().getAccession()).build();
        } else {
            throw new IntegrationTestFailException("Private sample in name search, sample name: " + sample.getName(), Phase.TWO);
        }

        testExternalReferences();
        client.persistCuration(sample.getAccession(),
                Curation.build(null, null, null, Collections.singletonList(ExternalReference.build("http://www.ebi.ac.uk/ena/ERA123456"))),
                "self.BiosampleIntegrationTest");
    }

    @Override
    protected void phaseThree() {
        Sample sample = getSampleTest1();
        //check there was no side-effects
        fetchUniqueSampleByName(sample.getName());
    }

    @Override
    protected void phaseFour() {

    }

    @Override
    protected void phaseFive() {

    }

    private void testExternalReferences() {
/*
		URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUri())
				.pathSegment("externalreferences").build().toUri();

		log.info("GETting from " + uri);
		RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
		ResponseEntity<PagedResources<Resource<ExternalReference>>> response = restTemplate.exchange(request,
				new ParameterizedTypeReference<PagedResources<Resource<ExternalReference>>>() {
				});

		boolean testedSelf = false;
		PagedResources<Resource<ExternalReference>> paged = response.getBody();

		for (Resource<ExternalReference> externalReferenceResource : paged) {
			Link selfLink = externalReferenceResource.getLink("self");

			if (selfLink == null) {
				throw new RuntimeException("Must have self link on " + externalReferenceResource);
			}

			if (externalReferenceResource.getLink("samples") == null) {
				throw new RuntimeException("Must have samples link on " + externalReferenceResource);
			}

			if (!testedSelf) {
				URI uriLink = URI.create(selfLink.getHref());
				log.info("GETting from " + uriLink);
				RequestEntity<Void> requestLink = RequestEntity.get(uriLink).accept(MediaTypes.HAL_JSON).build();
				ResponseEntity<Resource<ExternalReference>> responseLink = restTemplate.exchange(requestLink,
						new ParameterizedTypeReference<Resource<ExternalReference>>() {
						});
				if (!responseLink.getStatusCode().is2xxSuccessful()) {
					throw new RuntimeException("Unable to follow self link on " + externalReferenceResource);
				}
				testedSelf = true;
			}
		}
*/
    }

    private Sample getSampleTest1() {
        String name = "RestExternalReferenceIntegration_sample_1";
        Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
        Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

        SortedSet<Attribute> attributes = new TreeSet<>();
        attributes.add(Attribute.build("Organism", "Human"));

        SortedSet<Relationship> relationships = new TreeSet<>();

        SortedSet<ExternalReference> externalReferences = new TreeSet<>();
        externalReferences.add(ExternalReference.build("http://ega-archive.org/datasets/1", new TreeSet<>(Collections.singleton("DUO:0000007"))));
        externalReferences.add(ExternalReference.build("http://ega-archive.org/metadata/2",
                new TreeSet<>(Arrays.asList("DUO:0000005", "DUO:0000001", "DUO:0000007"))));
        externalReferences.add(ExternalReference.build("http://www.hpscreg.eu/3"));
        externalReferences.add(ExternalReference.build("http://www.test.com/4"));
        externalReferences.add(ExternalReference.build("http://www.ebi.ac.uk/arrayexpress/5"));
        externalReferences.add(ExternalReference.build("http://www.test.com/6"));
        externalReferences.add(ExternalReference.build("http://www.ebi.ac.uk/biostudies/7"));
        externalReferences.add(ExternalReference.build("http://ega-archive.org/datasets/EGAD00001001600",
                new TreeSet<>(Arrays.asList("DUO:0000005", "DUO:0000014", "DUO:0000019", "DUO:0000026", "DUO:0000028"))));

        return new Sample.Builder(name).withDomain(defaultIntegrationSubmissionDomain)
                .withRelease(release).withUpdate(update)
                .withAttributes(attributes).withRelationships(relationships).withExternalReferences(externalReferences)
                .build();
    }
}
