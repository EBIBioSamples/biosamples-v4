package uk.ac.ebi.biosamples.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.biosamples.service.SampleToXmlConverter;

@RunWith(SpringRunner.class)
@JsonTest
public class XmlTest {

	@Autowired
	private ObjectMapper objectMapper;
		
	@Test
	public void testSerialize() throws Exception {
		SampleToXmlConverter sampleToXmlConverter = new SampleToXmlConverter();
		Sample jsonSample = objectMapper.readValue(this.getClass().getResource("/TEST1.json"), Sample.class);
		Document docFromJson = sampleToXmlConverter.convert(jsonSample);
		
        SAXReader reader = new SAXReader();
        Document docFromXml = reader.read(this.getClass().getResource("/TEST1.xml"));
		
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter( System.out, format );
        writer.write( docFromJson );
        writer.write( docFromXml );
        
	    XMLUnit.setIgnoreAttributeOrder(true);
	    XMLUnit.setIgnoreWhitespace(true);
	    XMLUnit.setIgnoreComments(true);
	    Diff diff = XMLUnit.compareXML(docFromJson.asXML(), docFromXml.asXML());
	    
	    assertThat(diff.similar()).isTrue();		
	}
}
