package uk.ac.ebi.biosamples.neo;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.junit4.SpringRunner;

import junit.framework.Assert;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoAttribute;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReferenceApplication;
import uk.ac.ebi.biosamples.neo.model.NeoRelationship;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.service.modelconverter.AttributeToNeoAttributeConverter;
import uk.ac.ebi.biosamples.neo.service.modelconverter.ExternalReferenceToNeoExternalReferenceConverter;
import uk.ac.ebi.biosamples.neo.service.modelconverter.RelationshipToNeoRelationshipConverter;
import uk.ac.ebi.biosamples.neo.service.modelconverter.SampleToNeoSampleConverter;

import static org.assertj.core.api.Assertions.*;

@RunWith(SpringRunner.class)
@JsonTest
public class SampleToNeoSampleConverterTest {

	private SampleToNeoSampleConverter sampleToNeoSampleConverter;


	private Logger log = LoggerFactory.getLogger(getClass());
	

    @Before
    public void setup() {
    	sampleToNeoSampleConverter = new SampleToNeoSampleConverter(new AttributeToNeoAttributeConverter(), 
    			new ExternalReferenceToNeoExternalReferenceConverter(),
    			new RelationshipToNeoRelationshipConverter());
    }
	
	@Test
	public void basicTest() {
		Sample sample = getSimpleSample();
		NeoSample neoSample = getNeoSimpleSample();
		NeoSample neoSampleConverted = sampleToNeoSampleConverter.convert(sample);
		log.info(""+sample);
		log.info(""+neoSample);
		log.info(""+neoSampleConverted);
		Assertions.assertThat(neoSampleConverted).isEqualTo(neoSample);
	}
	

	private Sample getSimpleSample()  {
		String name = "Test Sample";
		String accession = "TEST1";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0));

		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
		attributes.add(Attribute.build("age", "3", null, "year"));
		attributes.add(Attribute.build("organism part", "lung", null, null));
		attributes.add(Attribute.build("organism part", "heart", null, null));
		
		SortedSet<Relationship> relationships = new TreeSet<>();
		relationships.add(Relationship.build("TEST1", "derived from", "TEST2"));
		
		SortedSet<ExternalReference> externalReferences = new TreeSet<>();
		externalReferences.add(ExternalReference.build("http://www.google.com"));

		return Sample.build(name, accession, release, update, attributes, relationships, externalReferences);
	}
	

	private NeoSample getNeoSimpleSample() {
		String name = "Test Sample";
		String accession = "TEST1";
		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0));
		NeoSample neoSample = NeoSample.build(name, accession, release, update, null, null, null);
		
		neoSample.getAttributes().add(NeoAttribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
		neoSample.getAttributes().add(NeoAttribute.build("age", "3", null, "year"));
		neoSample.getAttributes().add(NeoAttribute.build("organism part", "lung", null, null));
		neoSample.getAttributes().add(NeoAttribute.build("organism part", "heart", null, null));
		
		neoSample.getRelationships().add(NeoRelationship.build(NeoSample.create("TEST1"), "derived from", NeoSample.create("TEST2")));
		
		neoSample.getExternalReferenceApplications().add(NeoExternalReferenceApplication.build(neoSample, NeoExternalReference.build("http://www.google.com")));

		return neoSample;
	}
	
	@SpringBootConfiguration
	public static class TestConfig {
		
	}
}
