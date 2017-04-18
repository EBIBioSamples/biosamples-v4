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
import uk.ac.ebi.biosamples.neo.model.NeoRelationship;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.service.AttributeToNeoAttributeConverter;
import uk.ac.ebi.biosamples.neo.service.ExternalReferenceToNeoExternalReferenceConverter;
import uk.ac.ebi.biosamples.neo.service.RelationshipToNeoRelationshipConverter;
import uk.ac.ebi.biosamples.neo.service.SampleToNeoSampleConverter;

import static org.assertj.core.api.Assertions.*;

@RunWith(SpringRunner.class)
@JsonTest
public class AttributeToNeoAttributeConverterTest {

	private AttributeToNeoAttributeConverter attributeToNeoAttributeConverter;
	
    @Before
    public void setup() {
    	attributeToNeoAttributeConverter = new AttributeToNeoAttributeConverter();
    }
	
	@Test
	public void basicTest() {		
		Assertions.assertThat(attributeToNeoAttributeConverter.convert(
				Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null)))
			.isEqualTo(NeoAttribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
	}
	
	
	@SpringBootConfiguration
	public static class TestConfig {
		
	}
}
