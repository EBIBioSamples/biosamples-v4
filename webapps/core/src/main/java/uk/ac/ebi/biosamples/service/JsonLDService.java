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
package uk.ac.ebi.biosamples.service;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import uk.ac.ebi.biosamples.controller.SampleHtmlController;
import uk.ac.ebi.biosamples.controller.SampleRestController;
import uk.ac.ebi.biosamples.model.*;

/** This servise is meant for the conversions jobs to/form ld+json */
@Service
public class JsonLDService {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final ObjectMapper objectMapper;
  private final SampleToJsonLDSampleRecordConverter jsonLDSampleConverter;
  private String dataCatalogUrl;
  private String datasetUrl;

  public JsonLDService(final ObjectMapper mapper) {
    jsonLDSampleConverter = new SampleToJsonLDSampleRecordConverter();
    objectMapper = mapper;
    dataCatalogUrl = null;
    datasetUrl = null;
  }

  private void initUrls() {
    dataCatalogUrl = dataCatalogUrl == null ? getDataCatalogUrl() : dataCatalogUrl;
    datasetUrl = datasetUrl == null ? getDatasetUrl() : datasetUrl;
  }
  /**
   * Produce the ld+json version of a sample
   *
   * @param sample the sample to convert
   * @return the ld+json version of the sample
   */
  public JsonLDDataRecord sampleToJsonLD(final Sample sample) {
    final JsonLDDataRecord jsonLDDataRecord = jsonLDSampleConverter.convert(sample);
    final JsonLDSample jsonLDSample = jsonLDDataRecord.getMainEntity();

    try {
      final Method method =
          SampleRestController.class.getMethod(
              "getSampleHal", String.class, String.class, String[].class, String.class);
      final String sampleUrl =
          linkTo(method, sample.getAccession(), null, null, null).toUri().toString();
      jsonLDSample.setUrl(sampleUrl);
      jsonLDSample.setId(sampleUrl);
    } catch (final NoSuchMethodException e) {
      log.error("Failed to get method with reflections. ", e);
    }

    jsonLDDataRecord.mainEntity(jsonLDSample);

    return jsonLDDataRecord;
  }

  public JsonLDDataCatalog getBioSamplesDataCatalog() {
    initUrls();
    final JsonLDDataCatalog dataCatalog = new JsonLDDataCatalog();
    return dataCatalog.url(dataCatalogUrl).datasetUrl(datasetUrl);
  }

  public JsonLDDataset getBioSamplesDataset() {
    final JsonLDDataset dataset = new JsonLDDataset();
    initUrls();

    return dataset.datasetUrl(datasetUrl).dataCatalogUrl(dataCatalogUrl);
  }

  /**
   * Convert a ld+json sample to corresponding formatted json string
   *
   * @param jsonld the ld+json object
   * @return the formatted string representing the ld+json object
   */
  public String jsonLDToString(final BioschemasObject jsonld) {

    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonld);
    } catch (final JsonProcessingException e) {
      e.printStackTrace();
      return toString();
    }
  }

  private String getDataCatalogUrl() {

    String dataCatalogUrl = null;
    try {
      final Method method = SampleHtmlController.class.getMethod("index", Model.class);
      dataCatalogUrl = linkTo(method, null, null).toUri().toString();
    } catch (final NoSuchMethodException e) {
      e.printStackTrace();
    }
    return dataCatalogUrl;
  }

  private String getDatasetUrl() {
    String datasetUrl = null;
    try {
      final Method method =
          SampleHtmlController.class.getMethod(
              "samples",
              Model.class,
              String.class,
              String[].class,
              Integer.class,
              Integer.class,
              HttpServletRequest.class,
              HttpServletResponse.class);
      datasetUrl = linkTo(method, null, null, null, null, null, null, null).toUri().toString();
    } catch (final NoSuchMethodException e) {
      e.printStackTrace();
    }

    return datasetUrl;
  }
}
