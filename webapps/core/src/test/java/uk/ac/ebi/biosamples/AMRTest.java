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
package uk.ac.ebi.biosamples;

import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.auth.SubmissionAccount;
import uk.ac.ebi.biosamples.model.structured.amr.AMREntry;
import uk.ac.ebi.biosamples.model.structured.amr.AMRTable;
import uk.ac.ebi.biosamples.model.structured.amr.AmrPair;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.SchemaValidationService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationService;
import uk.ac.ebi.biosamples.validation.ElixirSchemaValidator;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AMRTest {

  /*
  @Autowired
  private SampleRestController sampleRestController;

  @Test
  public void contextLoads() throws Exception {
      assertThat(sampleRestController).isNotNull();
  }
  */
  @Autowired private MockMvc mockMvc;

  private JacksonTester<Sample> json;

  private ObjectMapper mapper;

  @MockBean private BioSamplesAapService bioSamplesAapService;

  @MockBean private BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;

  @MockBean private SampleService sampleService;

  @MockBean private SchemaValidationService schemaValidatorService;

  @MockBean ElixirSchemaValidator validator;

  private AMREntry getAMREntry() {
    return new AMREntry.Builder()
        .withAntibioticName(new AmrPair("ampicillin", ""))
        .withResistancePhenotype("susceptible")
        .withMeasure("==", "10", "mg/L")
        .withVendor("in-house")
        .withLaboratoryTypingMethod("CMAD")
        .withAstStandard("CLD")
        .build();
  }

  private AMRTable getAMRTable() {
    return new AMRTable.Builder("http://schema.org", "self.test", null)
        .addEntry(getAMREntry())
        .build();
  }

  private Sample.Builder getTestSampleBuilder() {
    return new Sample.Builder("testSample", "TEST1")
        .withDomain("foozit")
        .withRelease(Instant.now())
        .withUpdate(Instant.now());
  }

  @Before
  public void init() {
    mapper = new ObjectMapper();
  }

  @Test
  public void givenSample_whenGetSample_thenReturnJsonObject() throws Exception {
    Sample sample = getTestSampleBuilder().build();

    when(sampleService.fetch(eq(sample.getAccession()), any(), any(String.class)))
        .thenReturn(Optional.of(sample));
    when(bioSamplesAapService.isWriteSuperUser()).thenReturn(true);

    mockMvc
        .perform(
            get("/samples/{accession}", sample.getAccession())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is(sample.getName())))
        .andExpect(jsonPath("$.accession", is(sample.getAccession())));
  }

  @Test
  public void givenSampleWithStructuredData_whenGetSample_thenReturnStructuredDataInJson()
      throws Exception {
    AMREntry amrEntry = getAMREntry();
    AMRTable amrTable =
        new AMRTable.Builder("http://schema.org", "self.test", null).addEntry(amrEntry).build();

    Sample sample = getTestSampleBuilder().addData(amrTable).build();
    when(sampleService.fetch(eq(sample.getAccession()), any(), any(String.class)))
        .thenReturn(Optional.of(sample));
    when(bioSamplesAapService.isWriteSuperUser()).thenReturn(true);

    mockMvc
        .perform(
            get("/samples/{accession}", sample.getAccession()).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").exists())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(
            jsonPath("$.data[0]")
                .value(
                    allOf(
                        hasEntry("schema", "http://schema.org"),
                        hasEntry("type", "AMR"),
                        hasKey("content"))))
        .andExpect(jsonPath("$.data[0].content").isArray())
        .andExpect(
            jsonPath("$.data[0].content[0]")
                .value(
                    allOf(
                        hasEntry("resistance_phenotype", amrEntry.getResistancePhenotype()),
                        hasEntry("measurement_sign", amrEntry.getMeasurementSign()),
                        hasEntry("measurement_units", amrEntry.getMeasurementUnits()),
                        hasEntry("vendor", amrEntry.getVendor()),
                        hasEntry("laboratory_typing_method", amrEntry.getLaboratoryTypingMethod()),
                        hasEntry("ast_standard", amrEntry.getAstStandard()))))
        .andExpect(
            jsonPath("$.data[0].content[0].antibiotic_name")
                .value(hasEntry("value", amrEntry.getAntibioticName().getValue())))
        .andExpect(
            jsonPath("$.data[0].content[0]")
                .value(
                    hasEntry("measurement", amrEntry.getMeasurement()) // This needs to go here
                    // because the the
                    // hasEntry has
                    // a different signature - Only one having a number
                    // as a value. allOf wants all matchers of the same
                    // type
                    ));
  }

  @Test
  public void able_to_submit_sample_with_structuredData() throws Exception {

    String json =
        StreamUtils.copyToString(
            new ClassPathResource("amr_sample.json").getInputStream(), Charset.defaultCharset());
    JsonNode jsonSample = mapper.readTree(json);

    JsonNode jsonAmr = jsonSample.at("/data/0/content/0");
    AMREntry amrEntry =
        new AMREntry.Builder()
            .withAntibioticName(new AmrPair(jsonAmr.get("antibiotic_name").asText(), ""))
            .withResistancePhenotype(jsonAmr.get("resistance_phenotype").asText())
            .withMeasure(
                jsonAmr.get("measurement_sign").asText(),
                jsonAmr.get("measurement").asText(),
                jsonAmr.get("measurement_units").asText())
            .withVendor(jsonAmr.get("vendor").asText())
            .withLaboratoryTypingMethod(jsonAmr.get("laboratory_typing_method").asText())
            .withAstStandard(jsonAmr.get("ast_standard").asText())
            .build();

    Attribute organismAttribute = Attribute.build("organism", "Homo Sapiens");

    Sample testSample =
        new Sample.Builder(jsonSample.at("/name").asText())
            .withDomain(jsonSample.at("/domain").asText())
            .withUpdate(jsonSample.at("/update").asText())
            .withRelease(jsonSample.at("/release").asText())
            .addData(
                new AMRTable.Builder(jsonSample.at("/data/0/schema").asText(), "self.test", null)
                    .addEntry(amrEntry)
                    .build())
            .withAttributes(Collections.singletonList(organismAttribute))
            .build();

    when(bioSamplesAapService.isWriteSuperUser()).thenReturn(true);
    when(bioSamplesAapService.handleSampleDomain(any(Sample.class))).thenReturn(testSample);
    when(bioSamplesAapService.handleStructuredDataDomainInData(any(Sample.class)))
        .thenReturn(testSample);
    when(sampleService.store(testSample, true, "AAP")).thenReturn(testSample);

    mockMvc
        .perform(post("/samples").contentType(MediaType.APPLICATION_JSON_VALUE).content(json))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data[0].content").isArray());
  }

  @Test
  public void able_to_submit_amr_if_user_is_not_superuser() throws Exception {

    String json =
        StreamUtils.copyToString(
            new ClassPathResource("amr_sample.json").getInputStream(), Charset.defaultCharset());
    JsonNode jsonSample = mapper.readTree(json);

    JsonNode jsonAmr = jsonSample.at("/data/0/content/0");
    AMREntry amrEntry =
        new AMREntry.Builder()
            .withAntibioticName(new AmrPair(jsonAmr.get("antibiotic_name").asText(), ""))
            .withResistancePhenotype(jsonAmr.get("resistance_phenotype").asText())
            .withMeasure(
                jsonAmr.get("measurement_sign").asText(),
                jsonAmr.get("measurement").asText(),
                jsonAmr.get("measurement_units").asText())
            .withVendor(jsonAmr.get("vendor").asText())
            .withLaboratoryTypingMethod(jsonAmr.get("laboratory_typing_method").asText())
            .withAstStandard(jsonAmr.get("ast_standard").asText())
            .build();

    Attribute organismAttribute = Attribute.build("organism", "Homo Sapiens");

    Sample testSample =
        new Sample.Builder(jsonSample.at("/name").asText())
            .withDomain(jsonSample.at("/domain").asText())
            .withUpdate(jsonSample.at("/update").asText())
            .withRelease(jsonSample.at("/release").asText())
            .addData(
                new AMRTable.Builder(jsonSample.at("/data/0/schema").asText(), "self.test", null)
                    .addEntry(amrEntry)
                    .build())
            .withAttributes(Collections.singletonList(organismAttribute))
            .build();

    when(bioSamplesAapService.isWriteSuperUser()).thenReturn(false);
    when(bioSamplesAapService.handleSampleDomain(any(Sample.class))).thenReturn(testSample);
    when(bioSamplesAapService.handleStructuredDataDomainInData(any(Sample.class)))
        .thenReturn(testSample);

    ArgumentCaptor<Sample> generatedSample = ArgumentCaptor.forClass(Sample.class);
    when(sampleService.store(generatedSample.capture(), eq(true), eq("AAP")))
        .thenReturn(testSample);

    mockMvc.perform(post("/samples").contentType(MediaType.APPLICATION_JSON_VALUE).content(json));

    assert (!generatedSample.getValue().getData().isEmpty());
  }

  @Test
  public void able_to_submit_amr_with_webin_id() throws Exception {

    String json =
        StreamUtils.copyToString(
            new ClassPathResource("amr_sample_webin.json").getInputStream(),
            Charset.defaultCharset());
    JsonNode jsonSample = mapper.readTree(json);

    JsonNode jsonAmr = jsonSample.at("/data/0/content/0");
    SubmissionAccount submissionAccount = new SubmissionAccount();

    submissionAccount.setId("Webin-57176");

    AMREntry amrEntry =
        new AMREntry.Builder()
            .withAntibioticName(new AmrPair(jsonAmr.get("antibiotic_name").asText(), ""))
            .withResistancePhenotype(jsonAmr.get("resistance_phenotype").asText())
            .withMeasure(
                jsonAmr.get("measurement_sign").asText(),
                jsonAmr.get("measurement").asText(),
                jsonAmr.get("measurement_units").asText())
            .withVendor(jsonAmr.get("vendor").asText())
            .withLaboratoryTypingMethod(jsonAmr.get("laboratory_typing_method").asText())
            .withAstStandard(jsonAmr.get("ast_standard").asText())
            .build();

    Attribute organismAttribute = Attribute.build("organism", "Homo Sapiens");

    Sample testSample =
        new Sample.Builder(jsonSample.at("/name").asText())
            .withUpdate(jsonSample.at("/update").asText())
            .withRelease(jsonSample.at("/release").asText())
            .addData(
                new AMRTable.Builder(
                        jsonSample.at("/data/0/schema").asText(), "null", "Webin-57176")
                    .addEntry(amrEntry)
                    .build())
            .withAttributes(Collections.singletonList(organismAttribute))
            .build();

    when(bioSamplesWebinAuthenticationService.getWebinSubmissionAccount(any(String.class)))
        .thenReturn(ResponseEntity.ok(submissionAccount));
    when(bioSamplesWebinAuthenticationService.handleWebinUser(any(Sample.class), any(String.class)))
        .thenReturn(testSample);
    when(bioSamplesWebinAuthenticationService.handleStructuredDataForWebinSubmission(
            any(Sample.class), eq("Webin-57176")))
        .thenReturn(testSample);

    ArgumentCaptor<Sample> generatedSample = ArgumentCaptor.forClass(Sample.class);
    when(sampleService.store(generatedSample.capture(), eq(true), eq("WEBIN")))
        .thenReturn(testSample);

    mockMvc.perform(
        post("/samples?authProvider=WEBIN")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(json)
            .header("Authorization", "Bearer $TOKEN"));

    assert (!generatedSample.getValue().getData().isEmpty());
  }
}
