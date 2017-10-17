package uk.ac.ebi.biosamples.utils;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class XmlPathBuilderTest {

	private Document doc;
	
	@Before
	public void setup() {

        doc = DocumentHelper.createDocument();
        
		Element root = doc.addElement("ROOT");
		Element sample = root.addElement("SAMPLE");
		sample.addAttribute("alias", "ABC");
		Element identifiers = sample.addElement("IDENTIFIERS");
		Element primaryId = identifiers.addElement("PRIMARY_ID");
		primaryId.setText("ABC123");
		
	}
	
	@Test
	public void doRootTest() {
		Assert.assertEquals(XmlPathBuilder.of(doc).element().getName(), "ROOT");
	}
		
	@Test
	public void doLevel1Test() {
		Assert.assertTrue(XmlPathBuilder.of(doc).path("SAMPLE").exists());
		Assert.assertEquals(XmlPathBuilder.of(doc).path("SAMPLE").element().getName(), "SAMPLE");		
		Assert.assertTrue(XmlPathBuilder.of(doc).path("SAMPLE").attributeExists("alias"));		
		Assert.assertFalse(XmlPathBuilder.of(doc).path("SAMPLE").attributeExists("foo"));		
		Assert.assertEquals(XmlPathBuilder.of(doc).path("SAMPLE").attribute("alias"), "ABC");
		
		Assert.assertEquals(XmlPathBuilder.of(doc).path("SAMPLE").elements().size(), 1);
	}
	
	@Test
	public void doLevel2Test() {
		Assert.assertTrue(XmlPathBuilder.of(doc).path("SAMPLE", "IDENTIFIERS").exists());
		Assert.assertEquals(XmlPathBuilder.of(doc).path("SAMPLE", "IDENTIFIERS").element().getName(), "IDENTIFIERS");	
		Assert.assertEquals(XmlPathBuilder.of(doc).path("SAMPLE", "IDENTIFIERS", "PRIMARY_ID").text(), "ABC123");	
		
		
		Element idents = XmlPathBuilder.of(doc).path("SAMPLE", "IDENTIFIERS").element();
		Assert.assertTrue(XmlPathBuilder.of(idents).path("PRIMARY_ID").exists());
		

		Assert.assertEquals(XmlPathBuilder.of(doc).path("SAMPLE", "IDENTIFIERS").elements().size(), 1);
		Assert.assertEquals(XmlPathBuilder.of(doc).path("SAMPLE", "IDENTIFIERS").elements("PRIMARY_ID").size(), 1);
		
		

		for (Element e : XmlPathBuilder.of(doc).path("SAMPLE", "IDENTIFIERS").elements("PRIMARY_ID")) {
			Assert.assertEquals(XmlPathBuilder.of(e).text(), "ABC123");
		}
	}
}
