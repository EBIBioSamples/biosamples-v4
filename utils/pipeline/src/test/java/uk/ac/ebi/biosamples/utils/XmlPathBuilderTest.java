/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
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
    Assert.assertEquals(
        XmlPathBuilder.of(doc).path("SAMPLE", "IDENTIFIERS").element().getName(), "IDENTIFIERS");
    Assert.assertEquals(
        XmlPathBuilder.of(doc).path("SAMPLE", "IDENTIFIERS", "PRIMARY_ID").text(), "ABC123");

    Element idents = XmlPathBuilder.of(doc).path("SAMPLE", "IDENTIFIERS").element();
    Assert.assertTrue(XmlPathBuilder.of(idents).path("PRIMARY_ID").exists());

    Assert.assertEquals(XmlPathBuilder.of(doc).path("SAMPLE", "IDENTIFIERS").elements().size(), 1);
    Assert.assertEquals(
        XmlPathBuilder.of(doc).path("SAMPLE", "IDENTIFIERS").elements("PRIMARY_ID").size(), 1);

    for (Element e : XmlPathBuilder.of(doc).path("SAMPLE", "IDENTIFIERS").elements("PRIMARY_ID")) {
      Assert.assertEquals(XmlPathBuilder.of(e).text(), "ABC123");
    }
  }
}
