package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

import java.time.Instant;
import java.util.*;

@Component
@Order(1)
//@Profile({"default", "rest"})
public class RestSearchIntegration extends AbstractIntegration {

	private Logger log = LoggerFactory.getLogger(this.getClass());


	public RestSearchIntegration(BioSamplesClient client) {
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

		resource = client.persistSampleResource(test5);
		// Build inverse relationships for sample5
		SortedSet<Relationship> test5AllRelationships = test5.getRelationships();
		test5AllRelationships.add(Relationship.build(test4.getAccession(), "derive to", test5.getAccession()));
//		test5 = Sample.build(test5.getName(), test5.getAccession(), test5.getDomain(), test5.getRelease(), test5.getUpdate(),
//				test5.getCharacteristics(), test5AllRelationships, test5.getExternalReferences(), null, null, null);
        test5 = Sample.Builder.fromSample(test5).withRelationships(test5AllRelationships).build();
		if (!test5.equals(resource.getContent())) {
			throw new RuntimeException("Expected response ("+resource.getContent()+") to equal submission ("+test5+")");
		}

	}

	@Override
	protected void phaseTwo() {
	    Sample test1 = getSampleTest1();
	    Sample test2 = getSampleTest2();

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
			log.trace(""+resource);
			if (resource.getContent().getAccession().equals(test1.getAccession())) {
				throw new RuntimeException("Found non-public sample "+test1.getAccession()+" in search samples");
			}
		}

		//TODO check OLS expansion by making sure we can find the submitted samples in results for Eukaryota
		Set<String> accessions = new HashSet<>();
		for (Resource<Sample> sample : client.fetchSampleResourceAll("Eukaryota")) {
			accessions.add(sample.getContent().getAccession());
		}
		if (!accessions.contains(test2.getAccession())) {
			throw new RuntimeException(test2.getAccession() + " not found in search results for Eukaryota");
		}
	}

	@Override
	protected void phaseThree() {
		Sample sample2 = getSampleTest2();
		Sample sample4 = getSampleTest4();
		Sample sample5 = getSampleTest5();

        List<String> sample2ExpectedSearchResults = Arrays.asList(sample2.getAccession(), sample4.getAccession());
        List<String> sample4ExpectedSearchResults = Arrays.asList(sample2.getAccession(), sample4.getAccession(), sample5.getAccession());

		// Get results for sample2
		List<String> sample2EffectiveSearchResults = new ArrayList<>();
		for (Resource<Sample> sample : client.fetchSampleResourceAll(sample2.getAccession())) {
			sample2EffectiveSearchResults.add(sample.getContent().getAccession());
		}

		if (sample2EffectiveSearchResults.size() <= 0) {
			throw new RuntimeException("No search results found!");
		}

		if (!sample2EffectiveSearchResults.containsAll(sample2ExpectedSearchResults)) {
			throw new RuntimeException("Search results for " + sample2.getAccession() + " does not contains all expected samples");
		}

		// Get results for sample4
		List<String> sample4EffectiveSearchResults = new ArrayList<>();
		for (Resource<Sample> sample : client.fetchSampleResourceAll(sample4.getAccession())) {
			sample4EffectiveSearchResults.add(sample.getContent().getAccession());
		}

		if (sample4EffectiveSearchResults.size() <= 0) {
			throw new RuntimeException("No search results found!");
		}


		for (String expectedAccession : sample4ExpectedSearchResults) {
	        if (!sample4EffectiveSearchResults.contains(expectedAccession)) {
				throw new RuntimeException("Search results for " + sample4.getAccession() + " does not contains expected sample "+expectedAccession);
			}
		}

	}

	@Override
	protected void phaseFour() {
	}


	@Override
	protected void phaseFive() { }

	private Sample getSampleTest1() {
		String name = "Test Sample";
		String accession = "TESTrestsearch1";
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
		String accession = "SAMEA2";
		String domain = "self.BiosampleIntegrationTest";
		Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
		Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
				Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

		SortedSet<Relationship> relationships = new TreeSet<>();
		relationships.add(Relationship.build("SAMEA2", "derived from", "SAMEA3"));

//		return Sample.build(name, accession, "self.BiosampleIntegrationTest", release, update, attributes, relationships, new TreeSet<>(), null, null, null);
		return new Sample.Builder(name, accession).withDomain(domain).withRelease(release).withUpdate(update)
				.withAttributes(attributes).build();
	}

	private Sample getSampleTest4() {
		String name = "Test Sample the fourth";
		String accession = "SAMEA4";
		String domain = "self.BiosampleIntegrationTest";
		Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
		Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
				Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

		// TODO need to add inverse relationships later
		SortedSet<Relationship> relationships = new TreeSet<>();
		relationships.add(Relationship.build("SAMEA4", "derived from", getSampleTest2().getAccession()));
		relationships.add(Relationship.build("SAMEA4", "derive to", getSampleTest5().getAccession()));

//		return Sample.build(name, accession, "self.BiosampleIntegrationTest", release, update, attributes, relationships, new TreeSet<>(), null, null, null);
		return new Sample.Builder(name, accession).withDomain(domain).withRelease(release).withUpdate(update)
				.withAttributes(attributes).withRelationships(relationships).build();
	}

	private Sample getSampleTest5() {
		String name = "Test Sample the fifth";
		String accession = "SAMEA5";
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
