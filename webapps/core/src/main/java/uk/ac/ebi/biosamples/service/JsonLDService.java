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

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import uk.ac.ebi.biosamples.controller.SampleHtmlController;
import uk.ac.ebi.biosamples.controller.SampleRestController;
import uk.ac.ebi.biosamples.model.*;

/** This servise is meant for the convertions jobs to/form ld+json */
@Service
public class JsonLDService {

  private final ObjectMapper objectMapper;
  private final SampleToJsonLDSampleRecordConverter jsonLDSampleConverter;
  private String dataCatalogUrl;
  private String datasetUrl;

  public JsonLDService(ObjectMapper mapper) {
    this.jsonLDSampleConverter = new SampleToJsonLDSampleRecordConverter();
    this.objectMapper = mapper;
    this.dataCatalogUrl = null;
    this.datasetUrl = null;
  }

  private void initUrls() {
    this.dataCatalogUrl =
        this.dataCatalogUrl == null ? getDataCatalogUrl() : this.getDataCatalogUrl();
    this.datasetUrl = this.datasetUrl == null ? getDatasetUrl() : this.datasetUrl;
  }
  /**
   * Produce the ld+json version of a sample
   *
   * @param sample the sample to convert
   * @return the ld+json version of the sample
   */
  public JsonLDDataRecord sampleToJsonLD(Sample sample) {
    JsonLDDataRecord jsonLDDataRecord = this.jsonLDSampleConverter.convert(sample);
    JsonLDSample jsonLDSample = jsonLDDataRecord.getMainEntity();

    try {
      Method method =
          SampleRestController.class.getMethod(
              "getSampleHal", String.class, String.class, String[].class, String.class);
      String sampleUrl = linkTo(method, sample.getAccession()).toUri().toString();
      jsonLDSample.setUrl(sampleUrl);
      jsonLDSample.setId(sampleUrl);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }

    jsonLDDataRecord.mainEntity(jsonLDSample);
    return jsonLDDataRecord;
  }

  public JsonLDDataCatalog getBioSamplesDataCatalog() {

    initUrls();
    JsonLDDataCatalog dataCatalog = new JsonLDDataCatalog();
    return dataCatalog.url(this.dataCatalogUrl).datasetUrl(this.datasetUrl);
  }

  public JsonLDDataset getBioSamplesDataset() {
    JsonLDDataset dataset = new JsonLDDataset();
    initUrls();

    return dataset.datasetUrl(this.datasetUrl).dataCatalogUrl(this.dataCatalogUrl);
  }

  /**
   * Convert a ld+json sample to corresponding formatted json string
   *
   * @param jsonld the ld+json object
   * @return the formatted string representing the ld+json object
   */
  public String jsonLDToString(BioschemasObject jsonld) {

    try {
      return this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonld);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return this.toString();
    }
  }

  private String getDataCatalogUrl() {

    String dataCatalogUrl = null;
    try {
      Method method = SampleHtmlController.class.getMethod("index", Model.class);
      dataCatalogUrl = linkTo(method, null, null).toUri().toString();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }
    return dataCatalogUrl;
  }

  private String getDatasetUrl() {
    String datasetUrl = null;
    try {
      Method method =
          SampleHtmlController.class.getMethod(
              "samples",
              Model.class,
              String.class,
              String[].class,
              Integer.class,
              Integer.class,
              String.class,
              HttpServletRequest.class,
              HttpServletResponse.class);
      datasetUrl = linkTo(method, null, null, null, null, null, null, null).toUri().toString();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }

    return datasetUrl;
  }
}
