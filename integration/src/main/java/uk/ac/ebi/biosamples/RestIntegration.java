package uk.ac.ebi.biosamples;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@Component
public class RestIntegration extends AbstractIntegration {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	private Sample sampleTest1 = getSampleTest1();
	private Sample sampleTest2 = getSampleTest2();
	private Sample sampleTest3 = getSampleTest3();
	
	public RestIntegration(BioSamplesClient client) {
		super(client);
	}
	
	@Override
	protected void phaseOne() {		
		// get and check that nothing exists already
		Optional<Resource<Sample>> optional = client.fetchSampleResource(sampleTest1.getAccession());
		if (optional.isPresent()) {
			throw new RuntimeException("Found existing "+sampleTest1.getAccession());
		}
		
		// put a sample
		Resource<Sample> resource = client.persistSampleResource(sampleTest1);
		if (!sampleTest1.equals(resource.getContent())) {
			throw new RuntimeException("Expected response to equal submission");
		}		
		
		//put a private sample
		Sample sampleTest3 = getSampleTest3();
		client.persistSampleResource(sampleTest3);		
	}
	
	@Override
	protected void phaseTwo() {		
		// get to check it worked
		Optional<Resource<Sample>> optionalSample1 = client.fetchSampleResource(sampleTest1.getAccession());
		if (!optionalSample1.isPresent()) {
			throw new RuntimeException("No existing "+sampleTest1.getAccession());
		}
		if (!sampleTest1.equals(optionalSample1.get().getContent())) {
			throw new RuntimeException("Expected response to equal submission");
		}
		
		// check that the private sample is private
		Optional<Resource<Sample>> optionalSample3 = client.fetchSampleResource(sampleTest3.getAccession());
		if (optionalSample3.isPresent()) {
			throw new RuntimeException("Found existing "+sampleTest3.getAccession());
		}

		//put the second sample in
		Resource<Sample> resource = client.persistSampleResource(sampleTest2);
		if (!sampleTest2.equals(resource.getContent())) {
			throw new RuntimeException("Expected response to equal submission");
		}		
	}
	
	@Override
	protected void phaseThree() {
		//at this point, the inverse relationship should have been added		
		
		//check that it has the additional relationship added
		// get to check it worked
		Optional<Resource<Sample>> optionalSample2 = client.fetchSampleResource(sampleTest2.getAccession());
		if (!optionalSample2.isPresent()) {
			throw new RuntimeException("No existing "+sampleTest2.getAccession());
		}
		SortedSet<Relationship> sample2Relationships = optionalSample2.get().getContent().getRelationships();
		if (sample2Relationships.size() != 1) {
			log.warn("Non-sized "+optionalSample2.get().getContent());
			log.warn("Non-sized "+sample2Relationships);
			throw new RuntimeException("Expected one relationship");			
		}
		if (!sample2Relationships.iterator().next().equals(sampleTest1.getRelationships().iterator().next())) {
			throw new RuntimeException("Expected relationship on "+sampleTest2.getAccession()+" to match "+sampleTest1.getAccession());			
		}
		//check utf-8
		if (!optionalSample2.get().getContent().getCharacteristics().contains(Attribute.build("UTF-8 test", "αβ", null, null))) {
			throw new RuntimeException("Unable to find UTF-8 characters");
		}
		
		//get the original one to make sure it is there too
		Optional<Resource<Sample>> optionalSample1 = client.fetchSampleResource(sampleTest1.getAccession());
		if (!optionalSample1.isPresent()) {
			throw new RuntimeException("No existing "+sampleTest1.getAccession());
		}
		
		//now do another update to delete the relationship
		Sample updatedSampleTest1 = Sample.build(sampleTest1.getName(), sampleTest1.getAccession(),
				sampleTest1.getRelease(), sampleTest1.getUpdate(),
				sampleTest1.getCharacteristics(), new TreeSet<>(), sampleTest1.getExternalReferences());
		Resource<Sample> resource = client.persistSampleResource(updatedSampleTest1);
		if (!updatedSampleTest1.equals(resource.getContent())) {
			throw new RuntimeException("Expected response to equal submission");
		}
	}
	
	@Override
	protected void phaseFour() {	
		
	}
	
	@Override
	protected void phaseFive() {	
		//TODO check that relationship has been deleted
	}

	private Sample getSampleTest1() {
		String name = "Test Sample";
		String accession = "TESTrest1";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0));

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
			Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
		attributes.add(Attribute.build("age", "3", null, "year"));
		attributes.add(Attribute.build("organism part", "lung", null, null));
		attributes.add(Attribute.build("organism part", "heart", null, null));

		SortedSet<Relationship> relationships = new TreeSet<>();
		relationships.add(Relationship.build("TESTrest1", "derived from", "TESTrest2"));
		
		SortedSet<ExternalReference> externalReferences = new TreeSet<>();
		externalReferences.add(ExternalReference.build("http://www.google.com"));

		return Sample.build(name, accession, release, update, attributes, relationships, externalReferences);
	}

	private Sample getSampleTest2() {
		String name = "Test Sample the second";
		String accession = "TESTrest2";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0));

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
			Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
		attributes.add(Attribute.build("UTF-8 test", "αβ", null, null));

		return Sample.build(name, accession, release, update, attributes, new TreeSet<>(), new TreeSet<>());
	}

	private Sample getSampleTest3() {
		String name = "Test Sample the third";
		String accession = "TESTrest3";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2116, 4, 1), LocalTime.of(11, 36, 57, 0));

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
			Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

		return Sample.build(name, accession, release, update, attributes, new TreeSet<>(), new TreeSet<>());
	}

}
