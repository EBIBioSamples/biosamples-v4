package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Order(3)
@Profile({"default", "rest"})
public class RestSearchIntegration extends AbstractIntegration {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Sample test1 = getSampleTest1();
	private Sample test2 = getSampleTest2();
	private Sample test4 = getSampleTest4();
	
	public RestSearchIntegration(BioSamplesClient client) {
		super(client);
	}

	@Override
	protected void phaseOne() {
		//put a private sample
		Resource<Sample> resource = client.persistSampleResource(test1);
		if (!test1.equals(resource.getContent())) {
			throw new RuntimeException("Expected response ("+resource.getContent()+") to equal submission ("+test1+")");
		}
		//put a sample that refers to a non-existing sample
		resource = client.persistSampleResource(test2);
		if (!test2.equals(resource.getContent())) {
			throw new RuntimeException("Expected response ("+resource.getContent()+") to equal submission ("+test2+")");
		}

		resource = client.persistSampleResource(test4);
		if (!test4.equals(resource.getContent())) {
			throw new RuntimeException("Expected response ("+resource.getContent()+") to equal submission ("+test4+")");
		}
	}

	@Override
	protected void phaseTwo() {
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
			log.info(""+resource);
			if (resource.getContent().getAccession().equals(test1.getAccession())) {
				throw new RuntimeException("Found non-public sample "+test1.getAccession()+" in search samples");
			}
			if (resource.getContent().getAccession().equals("TESTrestsearch3")) {
				throw new RuntimeException("Found non-public sample TESTrestsearch3 in search samples");
			}
		}
	}

	@Override
	protected void phaseThree() {
		Sample sample2 = getSampleTest2();
		Sample sample4 = getSampleTest4();
		PagedResources<Resource<Sample>> pagedResources = client.search(getSampleTest2().getAccession());

		SortedSet<Relationship> sample2AllRelationships = sample2.getRelationships();
		sample2AllRelationships.addAll(sample4.getRelationships());

		sample2 = Sample.build(sample2.getName(), sample2.getAccession(), sample2.getRelease(), sample2.getUpdate(),
				sample2.getCharacteristics(), sample2AllRelationships, sample2.getExternalReferences());


		List<Sample> expectedSearchResults = Arrays.asList(sample2, sample4);

		List<Sample> searchResults = pagedResources.getContent().stream().map(Resource::getContent).collect(Collectors.toList());

		if (searchResults.size() <= 0) {
			throw new RuntimeException("No search results found!");
		}

		//check that the private sample is not in search results
		//check that the referenced non-existing sample not in search result
		for (Sample expectedSample: expectedSearchResults) {
			if (!searchResults.contains(expectedSample)) {
				throw new RuntimeException("Search results don't contains expected sample " + expectedSample.getAccession() + "!");
			}
		}

	}

	@Override
	protected void phaseFour() { }

	@Override
	protected void phaseFive() { }

	private Sample getSampleTest1() {
		String name = "Test Sample";
		String accession = "TESTrestsearch1";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2116, 4, 1), LocalTime.of(11, 36, 57, 0));

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
				Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

		return Sample.build(name, accession, release, update, attributes, new TreeSet<>(), new TreeSet<>());
	}

	private Sample getSampleTest2() {
		String name = "Test Sample the second";
		String accession = "TESTrestsearch2";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0));

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
				Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

		SortedSet<Relationship> relationships = new TreeSet<>();
		relationships.add(Relationship.build("TESTrestsearch2", "derived from", "TESTrestsearch3"));


		return Sample.build(name, accession, release, update, attributes, relationships, new TreeSet<>());
	}

	private Sample getSampleTest4() {
		String name = "Test Sample the fourth";
		String accession = "TESTrestsearch4";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0));

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
				Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

		// TODO need to add inverse relationships later
		SortedSet<Relationship> relationships = new TreeSet<>();
		relationships.add(Relationship.build("TESTrestsearch4", "derived from", "TESTrestsearch2"));

		return Sample.build(name, accession, release, update, attributes, relationships, new TreeSet<>());
	}

}
