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
package uk.ac.ebi.biosamples.docs;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.biosamples.core.model.Curation;
import uk.ac.ebi.biosamples.core.model.CurationLink;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.SampleStatus;
import uk.ac.ebi.biosamples.core.model.structured.StructuredData;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;
import uk.ac.ebi.biosamples.mongo.service.CurationReadService;
import uk.ac.ebi.biosamples.security.TestSecurityConfig;
import uk.ac.ebi.biosamples.security.model.AuthToken;
import uk.ac.ebi.biosamples.security.model.AuthorizationProvider;
import uk.ac.ebi.biosamples.security.service.AccessControlService;
import uk.ac.ebi.biosamples.service.*;
import uk.ac.ebi.biosamples.service.WebinAuthenticationService;
import uk.ac.ebi.biosamples.service.taxonomy.TaxonomyClientService;
import uk.ac.ebi.biosamples.service.validation.SchemaValidationService;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"spring.cloud.gcp.project-id=no_project"})
@AutoConfigureRestDocs
@ContextConfiguration(classes = TestSecurityConfig.class)
public class ApiDocumentationTest {
  private static final String WEBIN_TESTING_ACCOUNT = "WEBIN-12345";

  @Rule
  public final JUnitRestDocumentation restDocumentation =
      new JUnitRestDocumentation("target/generated-snippets");

  @Autowired private WebApplicationContext context;
  private ObjectMapper mapper;
  @MockBean private SamplePageService samplePageService;
  @MockBean private AccessControlService accessControlService;
  @MockBean private SampleService sampleService;
  @MockBean private CurationPersistService curationPersistService;
  @MockBean private CurationReadService curationReadService;
  @MockBean private WebinAuthenticationService webinAuthenticationService;
  @MockBean private TaxonomyClientService taxonomyClientService;
  @MockBean private SchemaValidationService schemaValidationService;
  @MockBean private StructuredDataService structuredDataService;

  private DocumentationHelper faker;
  private MockMvc mockMvc;
  private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  /**
   * Sets up the mock environment for the tests, configuring `MockMvc` with necessary settings for
   * the BioSamples API documentation.
   */
  @Before
  public void setUp() {
    faker = new DocumentationHelper();
    mapper = new ObjectMapper();
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(
                documentationConfiguration(restDocumentation)
                    .uris()
                    .withScheme("https")
                    .withHost("www.ebi.ac.uk")
                    .withPort(443))
            .defaultRequest(get("/").contextPath("/biosamples"))
            .build();
  }

  /**
   * Generates documentation snippets for the API root endpoint.
   *
   * @throws Exception if an error occurs while performing the request.
   */
  @Test
  public void getIndex() throws Exception {
    mockMvc
        .perform(get("/biosamples").accept(MediaTypes.HAL_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "get-index", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
  }

  /**
   * Generates documentation snippets for the samples root page.
   *
   * @throws Exception if an error occurs while performing the request.
   */
  @Test
  public void getSamples() throws Exception {
    final Sample fakeSample = faker.getExampleSample();
    when(samplePageService.getSamplesByText(
            nullable(String.class),
            anyList(),
            nullable(String.class),
            any(Pageable.class),
            anyBoolean()))
        .thenReturn(
            new PageImpl<>(Collections.singletonList(fakeSample), getDefaultPageable(), 100));
    when(samplePageService.getSamplesByText(
            nullable(String.class),
            anyList(),
            nullable(String.class),
            nullable(String.class),
            anyInt(),
            anyBoolean()))
        .thenReturn(new CursorArrayList<>(Collections.singletonList(fakeSample), ""));

    mockMvc
        .perform(get("/biosamples/samples").accept(MediaTypes.HAL_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "get-samples",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestParameters(
                    parameterWithName("cursor")
                        .description("Next page of a collection. Pass * for the first page")
                        .optional(),
                    parameterWithName("size").description("Entries per page").optional(),
                    parameterWithName("page")
                        .description("The page to retrieve. Not recommended for large results")
                        .optional(),
                    parameterWithName("text").description("Text to search").optional(),
                    parameterWithName("filter")
                        .description("List of filters to apply to search results")
                        .optional())));
  }

  /**
   * Tests posting a sample with minimal information, expecting a client error.
   *
   * @throws Exception if an error occurs during request execution.
   */
  @Test
  @WithUserDetails(WEBIN_TESTING_ACCOUNT)
  public void postSampleMinimalInfo() throws Exception {
    final String wrongSampleSerialized = "{\"name\": \"Sample without minimum information\" }";
    final Sample wrongSample =
        Sample.build(
            "Sample without minimum information",
            null,
            null,
            null,
            null,
            9606L,
            SampleStatus.PUBLIC,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    when(sampleService.getPrinciple(any(Authentication.class))).thenReturn(WEBIN_TESTING_ACCOUNT);
    when(webinAuthenticationService.handleWebinUserSubmission(
            any(Sample.class), any(), any(Optional.class)))
        .thenReturn(wrongSample);
    when(schemaValidationService.validate(any(Sample.class), any(String.class)))
        .thenReturn(wrongSample);
    when(taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(any(Sample.class)))
        .thenReturn(wrongSample);
    when(sampleService.persistSample(wrongSample, null, false))
        .thenThrow(GlobalExceptions.SampleMandatoryFieldsMissingException.class);
    mockMvc
        .perform(
            post("/biosamples/samples")
                .contentType(MediaType.APPLICATION_JSON)
                .content(wrongSampleSerialized)
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is4xxClientError())
        .andDo(
            document(
                "post-sample-minimal-information",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  /**
   * Tests posting a curation link with minimal information, expecting a client error.
   *
   * @throws Exception if an error occurs during request execution.
   */
  @Test
  public void postCurationLinkMinimalInfo() throws Exception {
    final String wrongSampleSerialized = "{\"sample\": \"SAMFAKE123456\", \"curation\": {}}";

    mockMvc
        .perform(
            post("/biosamples/samples")
                .contentType(MediaType.APPLICATION_JSON)
                .content(wrongSampleSerialized)
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is4xxClientError())
        .andDo(
            document(
                "post-curation-minimal-information",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  /**
   * Tests sample submission with a Webin authentication token, ensuring successful processing and
   * documentation generation.
   *
   * @throws Exception if an error occurs during request execution.
   */
  @Test
  @WithUserDetails(WEBIN_TESTING_ACCOUNT)
  public void postSample() throws Exception {
    final Sample sampleWithWebinId = faker.getNonAccessionedExampleSampleWithWebinId();
    final String sampleToSubmit =
        "{ "
            + "\"name\" : \""
            + sampleWithWebinId.getName()
            + "\", "
            + "\"release\" : \""
            + dateTimeFormatter.format(sampleWithWebinId.getRelease().atOffset(ZoneOffset.UTC))
            + "\", "
            + "\"webinSubmissionAccountId\" : \"Webin-12345\" "
            + "}";

    when(webinAuthenticationService.handleWebinUserSubmission(
            any(Sample.class), any(), any(Optional.class)))
        .thenReturn(sampleWithWebinId);
    when(sampleService.getPrinciple(any(Authentication.class))).thenReturn(WEBIN_TESTING_ACCOUNT);
    when(sampleService.persistSample(any(Sample.class), eq(null), eq(false)))
        .thenReturn(sampleWithWebinId);
    when(schemaValidationService.validate(any(Sample.class), any(String.class)))
        .thenReturn(sampleWithWebinId);
    when(taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(any(Sample.class)))
        .thenReturn(sampleWithWebinId);

    mockMvc
        .perform(
            post("/biosamples/samples")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleToSubmit)
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "post-sample",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  /**
   * Tests sample submission with external references, ensuring correct handling and documentation
   * generation.
   *
   * @throws Exception if an error occurs during request execution.
   */
  @Test
  @WithUserDetails(WEBIN_TESTING_ACCOUNT)
  public void postSampleWithExternalReferences() throws Exception {
    final Sample sample = faker.getExampleSampleWithExternalReferences();
    final String sampleToSubmit =
        "{ "
            + "\"name\" : \""
            + sample.getName()
            + "\", "
            + "\"release\" : \""
            + dateTimeFormatter.format(sample.getRelease().atOffset(ZoneOffset.UTC))
            + "\", "
            + "\"webinSubmissionAccountId\" : \"Webin-12345\", "
            + "\"externalReferences\" : [ { "
            + "    \"url\" : \"https://www.ebi.ac.uk/ena/data/view/SAMEA00001\" "
            + "  } ]"
            + "}";

    when(sampleService.getPrinciple(any(Authentication.class))).thenReturn(WEBIN_TESTING_ACCOUNT);
    when(webinAuthenticationService.handleWebinUserSubmission(
            any(Sample.class), any(String.class), eq(Optional.empty())))
        .thenReturn(sample);
    when(sampleService.persistSample(any(Sample.class), eq(null), eq(false))).thenReturn(sample);
    when(schemaValidationService.validate(any(Sample.class), any(String.class))).thenReturn(sample);
    when(taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(any(Sample.class)))
        .thenReturn(sample);

    mockMvc
        .perform(
            post("/biosamples/samples")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleToSubmit)
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "post-sample-with-external-references",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  /**
   * Generate the snippets for submitting structured data to BioSamples
   *
   * @throws Exception
   */
  @Test
  @WithUserDetails(WEBIN_TESTING_ACCOUNT)
  public void putStructuredData() throws Exception {
    final StructuredData structuredData = faker.getExampleStructuredData();

    when(sampleService.getPrinciple(any(Authentication.class))).thenReturn(WEBIN_TESTING_ACCOUNT);
    when(structuredDataService.saveStructuredData(eq(structuredData))).thenReturn(structuredData);
    when(structuredDataService.getStructuredData(eq(structuredData.getAccession())))
        .thenReturn(Optional.of(structuredData));
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256",
                    AuthorizationProvider.WEBIN,
                    WEBIN_TESTING_ACCOUNT,
                    Collections.emptyList())));

    mockMvc
        .perform(
            put("/biosamples/structureddata/" + structuredData.getAccession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(structuredData))
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "put-structured-data",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  /** Accessioning service to generate accession */
  @Test
  @WithUserDetails(WEBIN_TESTING_ACCOUNT)
  public void postToGenerateAccession() throws Exception {
    final Sample sampleWithWebinId = faker.getExampleSampleWithWebinId();
    final Instant release =
        Instant.ofEpochSecond(
            LocalDateTime.now(ZoneOffset.UTC).plusYears(100).toEpochSecond(ZoneOffset.UTC));
    final Sample sampleWithUpdatedDate =
        Sample.Builder.fromSample(sampleWithWebinId).withRelease(release).build();

    final String sampleToSubmit =
        "{ "
            + "\"name\" : \""
            + sampleWithWebinId.getName()
            + "\", "
            + "\"update\" : \""
            + dateTimeFormatter.format(sampleWithWebinId.getUpdate().atOffset(ZoneOffset.UTC))
            + "\" "
            //                "\"release\" : \""
            // +dateTimeFormatter.format(sample.getRelease().atOffset(ZoneOffset.UTC)) +
            // "\", " +
            + "}";

    when(webinAuthenticationService.handleWebinUserSubmission(
            any(Sample.class), any(String.class), eq(Optional.empty())))
        .thenReturn(sampleWithWebinId);
    when(sampleService.buildPrivateSample(any(Sample.class))).thenReturn(sampleWithUpdatedDate);
    when(sampleService.getPrinciple(any(Authentication.class))).thenReturn(WEBIN_TESTING_ACCOUNT);
    when(sampleService.persistSample(any(Sample.class), eq(null), eq(false)))
        .thenReturn(sampleWithUpdatedDate);
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.WEBIN, "WEBIN-12345", Collections.emptyList())));
    when(accessControlService.extractToken(null)).thenReturn(Optional.empty());

    mockMvc
        .perform(
            post("/biosamples/samples/accession")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleToSubmit)
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "accession-sample",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  /** Accessioning service to generate accession */
  @Test
  @WithUserDetails(WEBIN_TESTING_ACCOUNT)
  public void shouldReturnErrorWhenPostForAccessioningWithAccession() throws Exception {
    final Sample sampleWithWebinId = faker.getExampleSampleWithWebinId();
    final String sampleToSubmit =
        "{ "
            + "\"accession\" : \""
            + "FakeAccession"
            + "\", "
            + "\"name\" : \""
            + sampleWithWebinId.getName()
            + "\", "
            + "\"update\" : \""
            + dateTimeFormatter.format(sampleWithWebinId.getUpdate().atOffset(ZoneOffset.UTC))
            + "\", "
            + "\"release\" : \""
            + dateTimeFormatter.format(sampleWithWebinId.getRelease().atOffset(ZoneOffset.UTC))
            + "\", "
            + "\"webinId\" : \""
            + WEBIN_TESTING_ACCOUNT
            + "\""
            + " }";

    when(sampleService.getPrinciple(any(Authentication.class))).thenReturn(WEBIN_TESTING_ACCOUNT);
    when(sampleService.persistSample(any(Sample.class), eq(null), eq(false)))
        .thenReturn(sampleWithWebinId);

    mockMvc
        .perform(
            post("/biosamples/samples/accession")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleToSubmit)
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is4xxClientError())
        .andExpect(
            result ->
                assertTrue(
                    result.getResolvedException()
                        instanceof GlobalExceptions.SampleWithAccessionSubmissionException,
                    "Expected SampleWithAccessionSubmissionException but got: "
                        + result.getResolvedException()));
  }

  /** validation service to validate basic fields */
  @Test
  public void postForValidation() throws Exception {
    final Sample sample = faker.getExampleSample();
    final String sampleToSubmit =
        "{ "
            + "\"name\" : \""
            + "fake_sample"
            + "\", "
            + "\"update\" : \""
            + dateTimeFormatter.format(sample.getUpdate().atOffset(ZoneOffset.UTC))
            + "\", "
            + "\"release\" : \""
            + dateTimeFormatter.format(sample.getRelease().atOffset(ZoneOffset.UTC))
            + "\", "
            + "\"domain\" : \"self.ExampleDomain\" "
            + ", "
            + "\"characteristics\" : {"
            + "\"material\" : [ {"
            + "\"text\" : \"cell line\","
            + "\"ontologyTerms\" : [ \"EFO_0000322\" ]"
            + "} ],"
            + "\"Organism\" : [ {"
            + "\"text\" : \"Homo sapiens\","
            + "\"ontologyTerms\" : [ \"9606\" ]"
            + "} ],"
            + "\"checklist\" : [ {"
            + "\"text\" : \"BSDC00001\""
            + "} ]}"
            + "}";

    mockMvc
        .perform(
            post("/biosamples/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleToSubmit)
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "validate-sample",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  /** Recommendation service to suggest sample attributes */
  @Test
  public void postForRecommendation() throws Exception {
    final Sample sample = faker.getExampleSample();
    final String sampleToSubmit =
        "{ "
            + "\"name\" : \""
            + "fake_sample"
            + "\", "
            + "\"update\" : \""
            + dateTimeFormatter.format(sample.getUpdate().atOffset(ZoneOffset.UTC))
            + "\", "
            + "\"release\" : \""
            + dateTimeFormatter.format(sample.getRelease().atOffset(ZoneOffset.UTC))
            + "\", "
            + "\"domain\" : \"self.ExampleDomain\" "
            + ", "
            + "\"characteristics\" : {"
            + "\"material\" : [ {"
            + "\"text\" : \"cell line\","
            + "\"ontologyTerms\" : [ \"EFO_0000322\" ]"
            + "} ],"
            + "\"Organism\" : [ {"
            + "\"text\" : \"Homo sapiens\","
            + "\"ontologyTerms\" : [ \"9606\" ]"
            + "} ],"
            + "\"Gender\" : [ {"
            + "\"text\" : \"male\","
            + "\"ontologyTerms\" : [ \"PATO_0000384\" ]"
            + "} ]}"
            + "}";

    mockMvc
        .perform(
            post("/biosamples/recommendations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleToSubmit)
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "post-sample-for-suggestions",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  @Test
  @WithUserDetails(WEBIN_TESTING_ACCOUNT)
  public void putSample1() throws Exception {
    final Sample sampleWithWebinId = faker.getExampleSampleWithWebinId();

    when(sampleService.getPrinciple(any(Authentication.class))).thenReturn(WEBIN_TESTING_ACCOUNT);
    when(webinAuthenticationService.handleWebinUserSubmission(
            any(Sample.class), any(String.class), eq(Optional.of(sampleWithWebinId))))
        .thenReturn(sampleWithWebinId);
    when(sampleService.fetch(eq(sampleWithWebinId.getAccession()), eq(false)))
        .thenReturn(Optional.of(sampleWithWebinId));
    when(sampleService.persistSample(eq(sampleWithWebinId), eq(sampleWithWebinId), eq(false)))
        .thenReturn(sampleWithWebinId);
    when(taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(any(Sample.class)))
        .thenReturn(sampleWithWebinId);

    mockMvc
        .perform(
            put("/biosamples/samples/" + sampleWithWebinId.getAccession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(sampleWithWebinId))
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "put-sample", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
  }

  @Test
  @WithUserDetails(WEBIN_TESTING_ACCOUNT)
  public void putSample2() throws Exception {
    final Sample sampleWithWebinId = faker.getExampleSampleWithWebinId();

    when(sampleService.getPrinciple(any(Authentication.class))).thenReturn(WEBIN_TESTING_ACCOUNT);
    when(webinAuthenticationService.handleWebinUserSubmission(
            any(Sample.class), any(String.class), eq(Optional.of(sampleWithWebinId))))
        .thenReturn(sampleWithWebinId);
    when(sampleService.fetch(eq(sampleWithWebinId.getAccession()), eq(false)))
        .thenReturn(Optional.of(sampleWithWebinId));
    when(sampleService.isNotExistingAccession(sampleWithWebinId.getAccession())).thenReturn(false);
    when(sampleService.persistSample(eq(sampleWithWebinId), eq(sampleWithWebinId), eq(false)))
        .thenReturn(sampleWithWebinId);

    when(taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(any(Sample.class)))
        .thenReturn(sampleWithWebinId);

    mockMvc
        .perform(
            put("/biosamples/samples/" + sampleWithWebinId.getAccession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(sampleWithWebinId))
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "put-sample-2",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  /**
   * Generate the snippets for Sample submission to BioSamples with relationships
   *
   * @throws Exception
   */
  @Test
  @WithUserDetails(WEBIN_TESTING_ACCOUNT)
  public void putSampleWithRelationships() throws Exception {
    final Sample sampleWithWebinId = faker.getExampleSampleWithRelationships();

    when(sampleService.getPrinciple(any(Authentication.class))).thenReturn(WEBIN_TESTING_ACCOUNT);
    when(webinAuthenticationService.handleWebinUserSubmission(
            any(Sample.class), any(String.class), eq(Optional.of(sampleWithWebinId))))
        .thenReturn(sampleWithWebinId);
    when(sampleService.fetch(eq(sampleWithWebinId.getAccession()), eq(false)))
        .thenReturn(Optional.of(sampleWithWebinId));
    when(sampleService.persistSample(eq(sampleWithWebinId), eq(sampleWithWebinId), eq(false)))
        .thenReturn(sampleWithWebinId);
    when(taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(any(Sample.class)))
        .thenReturn(sampleWithWebinId);

    mockMvc
        .perform(
            put("/biosamples/samples/" + sampleWithWebinId.getAccession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(sampleWithWebinId))
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "put-sample-with-relationships",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  @Test
  @WithUserDetails(WEBIN_TESTING_ACCOUNT)
  public void postCurationLink() throws Exception {
    final CurationLink curationLink = faker.getExampleCurationLink();

    when(sampleService.getPrinciple(any(Authentication.class))).thenReturn(WEBIN_TESTING_ACCOUNT);
    when(webinAuthenticationService.handleWebinUserSubmission(
            any(CurationLink.class), any(String.class)))
        .thenReturn(curationLink);
    when(curationPersistService.store(any(CurationLink.class))).thenReturn(curationLink);

    mockMvc
        .perform(
            post("/biosamples/samples/{accession}/curationlinks", curationLink.getSample())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(curationLink))
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "post-curation",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  @Test
  public void getSample() throws Exception {
    final Sample sample =
        faker.getExampleSampleBuilder().withDomain(faker.getExampleDomain()).build();
    when(sampleService.fetch(sample.getAccession(), true)).thenReturn(Optional.of(sample));
    when(accessControlService.extractToken(anyString())).thenReturn(Optional.empty());

    mockMvc
        .perform(
            get("/biosamples/samples/{accession}", sample.getAccession())
                .accept(MediaTypes.HAL_JSON))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "get-sample", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
  }

  @Test
  public void getCurations() throws Exception {
    final Page<Curation> curationPage =
        new PageImpl<>(
            Collections.singletonList(faker.getExampleCurationLink().getCuration()),
            getDefaultPageable(),
            100);
    when(curationReadService.getPage(isA(Pageable.class))).thenReturn(curationPage);

    mockMvc
        .perform(get("/biosamples/curations").accept(MediaTypes.HAL_JSON))
        .andExpect(status().is2xxSuccessful())
        .andDo(document("get-curations"));
  }

  @Test
  public void getSampleCurationLinks() throws Exception {
    final Page<CurationLink> curationLinkPage =
        new PageImpl<>(
            Collections.singletonList(faker.getExampleCurationLink()), getDefaultPageable(), 100);
    final String sampleAccession = curationLinkPage.getContent().get(0).getSample();

    when(curationReadService.getCurationLinksForSample(eq(sampleAccession), isA(Pageable.class)))
        .thenReturn(curationLinkPage);

    mockMvc
        .perform(
            get("/biosamples/samples/{accession}/curationlinks", sampleAccession)
                .accept(MediaTypes.HAL_JSON))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "get-sample-curation",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  // TODO Move all the subsequent to DocumentationHelper class
  private Pageable getDefaultPageable() {
    return PageRequest.of(0, 1, getDefaultSort());
  }

  private Sort getDefaultSort() {
    return Sort.by(
        Stream.of("score,desc", "id,asc").map(this::parseSort).collect(Collectors.toList()));
  }

  private Sort.Order parseSort(final String sort) {
    if (sort.endsWith(",desc")) {
      return new Sort.Order(Sort.Direction.DESC, sort.substring(0, sort.length() - 5));
    } else if (sort.endsWith(",asc")) {
      return new Sort.Order(Sort.Direction.ASC, sort.substring(0, sort.length() - 4));
    } else {
      return new Sort.Order(null, sort);
    }
  }

  private String serialize(final Object obj) throws JsonProcessingException {
    return mapper.writeValueAsString(obj);
  }
}
