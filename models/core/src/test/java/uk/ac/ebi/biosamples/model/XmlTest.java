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
package uk.ac.ebi.biosamples.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.ac.ebi.biosamples.service.ExternalReferenceService;
import uk.ac.ebi.biosamples.service.SampleToXmlConverter;

@RunWith(SpringRunner.class)
@JsonTest
public class XmlTest {

  @Autowired private ObjectMapper objectMapper;

  @Test
  public void testSerialize() throws Exception {
    final ExternalReferenceService externalReferenceService = new ExternalReferenceService();
    final SampleToXmlConverter sampleToXmlConverter =
        new SampleToXmlConverter(externalReferenceService);
    final Sample jsonSample =
        objectMapper.readValue(getClass().getResource("/TEST1.json"), Sample.class);
    final Document docFromJson = sampleToXmlConverter.convert(jsonSample);

    final SAXReader reader = new SAXReader();
    final Document docFromXml = reader.read(getClass().getResource("/TEST1.xml"));

    final OutputFormat format = OutputFormat.createPrettyPrint();
    final XMLWriter writer = new XMLWriter(System.out, format);
    writer.write(docFromJson);
    writer.write(docFromXml);

    XMLUnit.setIgnoreAttributeOrder(true);
    XMLUnit.setIgnoreWhitespace(true);
    XMLUnit.setIgnoreComments(true);
    final Diff diff = XMLUnit.compareXML(docFromJson.asXML(), docFromXml.asXML());

    assertThat(diff.similar()).isTrue();
  }
}
