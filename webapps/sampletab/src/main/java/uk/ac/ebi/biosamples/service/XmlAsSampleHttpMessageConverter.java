/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import uk.ac.ebi.biosamples.model.Sample;

public class XmlAsSampleHttpMessageConverter extends AbstractHttpMessageConverter<Sample> {

  private final XmlSampleToSampleConverter xmlSampleToSampleConverter;
  private final XmlGroupToSampleConverter xmlGroupToSampleConverter;

  private final List<MediaType> DEFAULT_SUPPORTED_MEDIA_TYPES =
      Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML);

  private Logger log = LoggerFactory.getLogger(getClass());

  public XmlAsSampleHttpMessageConverter(
      XmlSampleToSampleConverter xmlSampleToSampleConverter,
      XmlGroupToSampleConverter xmlGroupToSampleConverter) {
    this.setSupportedMediaTypes(this.DEFAULT_SUPPORTED_MEDIA_TYPES);
    this.xmlSampleToSampleConverter = xmlSampleToSampleConverter;
    this.xmlGroupToSampleConverter = xmlGroupToSampleConverter;
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return Sample.class.isAssignableFrom(clazz);
  }

  @Override
  protected Sample readInternal(Class<? extends Sample> clazz, HttpInputMessage inputMessage)
      throws IOException, HttpMessageNotReadableException {

    log.info("Reached readInternal");

    SAXReader saxReader = new SAXReader();
    Document doc;
    try {
      doc = saxReader.read(inputMessage.getBody());
    } catch (DocumentException e) {
      throw new HttpMessageNotReadableException("error parsing xml", e);
    }

    if (doc.getRootElement().getName().equals("BioSample")) {
      log.info("converting BioSample");
      return xmlSampleToSampleConverter.convert(doc.getRootElement());
    } else if (doc.getRootElement().getName().equals("BioSampleGroup")) {
      log.info("converting BioSampleGroup");
      return xmlGroupToSampleConverter.convert(doc.getRootElement());
    } else {
      log.error("Unable to read message with root element " + doc.getRootElement().getName());
      throw new HttpMessageNotReadableException("Cannot recognize xml");
    }
  }

  @Override
  protected void writeInternal(Sample sample, HttpOutputMessage outputMessage)
      throws IOException, HttpMessageNotWritableException {
    throw new HttpMessageNotReadableException("Cannot write xml");
  }
}
