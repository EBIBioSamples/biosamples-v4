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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
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
import org.junit.Ignore;
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
import org.springframework.http.ResponseEntity;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.model.Certificate;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;
import uk.ac.ebi.biosamples.model.auth.SubmissionAccount;
import uk.ac.ebi.biosamples.model.certification.*;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.service.*;
import uk.ac.ebi.biosamples.service.certification.CertifyService;
import uk.ac.ebi.biosamples.service.security.AccessControlService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationService;
import uk.ac.ebi.biosamples.service.taxonomy.TaxonomyClientService;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;
import uk.ac.ebi.biosamples.utils.mongo.CurationReadService;
import uk.ac.ebi.biosamples.validation.SchemaValidationService;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureRestDocs
@TestPropertySource(properties = {"aap.domains.url = ''"})
public class ApiDocumentationTest {

  @Rule
  public final JUnitRestDocumentation restDocumentation =
      new JUnitRestDocumentation("target/generated-snippets");

  @Autowired private WebApplicationContext context;
  private ObjectMapper mapper;

  @MockBean private SamplePageService samplePageService;

  @MockBean private AccessControlService accessControlService;

  @MockBean private SampleService sampleService;

  @MockBean private CertifyService certifyService;

  @MockBean CurationPersistService curationPersistService;

  @MockBean CurationReadService curationReadService;

  @MockBean private BioSamplesAapService aapService;

  @MockBean private BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;

  @MockBean private TaxonomyClientService taxonomyClientService;

  @MockBean private SchemaValidationService schemaValidationService;

  @MockBean private StructuredDataService structuredDataService;

  private DocumentationHelper faker;
  private MockMvc mockMvc;
  private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  @Before
  public void setUp() {
    this.faker = new DocumentationHelper();
    this.mapper = new ObjectMapper();
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(this.context)
            .apply(
                documentationConfiguration(this.restDocumentation)
                    .uris()
                    .withScheme("https")
                    .withHost("www.ebi.ac.uk")
                    .withPort(443))
            .defaultRequest(get("/").contextPath("/biosamples"))
            .build();
  }

  /**
   * Generate the snippets for the API root
   *
   * @throws Exception
   */
  @Test
  public void getIndex() throws Exception {
    this.mockMvc
        .perform(get("/biosamples").accept(MediaTypes.HAL_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "get-index", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
  }

  /**
   * Generate the snippets for the samples root page
   *
   * @throws Exception
   */
  @Test
  public void getSamples() throws Exception {
    Sample fakeSample = this.faker.getExampleSample();
    when(samplePageService.getSamplesByText(nullable(String.class), anyList(), anySet(), nullable(String.class),
                                            any(Pageable.class), nullable(String.class), any()))
        .thenReturn(new PageImpl<>(Collections.singletonList(fakeSample), getDefaultPageable(), 100));

    when(samplePageService.getSamplesByText(nullable(String.class), anyList(), anySet(), nullable(String.class),
                                            nullable(String.class), anyInt(), any(), any()))
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
                    parameterWithName("page").description("Not recommended to use for pagination of large number of results").optional(),
                    parameterWithName("cursor").description("Next page of a collection. Pass * for the first page").optional(),
                    parameterWithName("size").description("Entries per page").optional(),
                    parameterWithName("text").description("Text to search").optional(),
                    parameterWithName("filter")
                        .description("List of filters to apply to search results")
                        .optional())));
  }

  /**
   * Describe what's the minimal information necessary to submit a sample
   *
   * @throws Exception
   */
  @Test
  @Ignore
  public void postSampleMinimalInfo() throws Exception {
    final ObjectMapper jsonMapper = new ObjectMapper();
    String wrongSampleSerialized = "{\"name\": \"Sample without minimum information\" }";
    Sample wrongSample =
        Sample.build(
            "Sample without minimum information",
            null,
            null,
            null,
            Long.valueOf(9606),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    when(aapService.handleSampleDomain(any(Sample.class))).thenReturn(wrongSample);
    when(sampleService.persistSample(wrongSample, null, false)).thenCallRealMethod();
    when(sampleService.persistSample(wrongSample, null, false)).thenCallRealMethod();
    when(certifyService.certify(jsonMapper.writeValueAsString(wrongSample), true))
        .thenReturn(Collections.emptyList());

    this.mockMvc
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
   * Describes what's the error when curationLink minimal information is not provided
   *
   * @throws Exception
   */
  @Test
  public void postCurationLinkMinimalInfo() throws Exception {

    String wrongSampleSerialized = "{\"sample\": \"SAMFAKE123456\", \"curation\": {}}";

    this.mockMvc
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
   * Generate the snippets for Sample submission to BioSamples
   *
   * @throws Exception
   */
  @Test
  public void postSample() throws Exception {
    Sample sample = this.faker.getExampleSample();
    Sample sampleWithDomain = this.faker.getExampleSampleWithDomain();

    String sampleToSubmit =
        "{ "
            + "\"name\" : \""
            + sample.getName()
            + "\", "
            + "\"release\" : \""
            + dateTimeFormatter.format(sample.getRelease().atOffset(ZoneOffset.UTC))
            + "\", "
            + "\"domain\" : \"self.ExampleDomain\" "
            + "}";

    when(aapService.handleSampleDomain(any(Sample.class))).thenReturn(sampleWithDomain);
    when(sampleService.persistSample(any(Sample.class), eq(AuthorizationProvider.AAP), eq(false)))
        .thenReturn(sampleWithDomain);
    when(schemaValidationService.validate(any(Sample.class))).thenReturn("BSDC00001");
    when(taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(
            any(Sample.class), eq(false)))
        .thenReturn(sampleWithDomain);
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.AAP, "user", Collections.emptyList())));
    when(accessControlService.extractToken(null)).thenReturn(Optional.empty());

    this.mockMvc
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
   * Generate the snippets for Sample submission to BioSamples
   *
   * @throws Exception
   */
  @Test
  public void postSampleWithWebinAuthentication() throws Exception {
    Sample sample = this.faker.getExampleSample();
    Sample sampleWithWebinId = this.faker.getExampleSampleWithWebinId();
    SubmissionAccount submissionAccount = new SubmissionAccount();
    submissionAccount.setId("WEBIN-12345");

    String sampleToSubmit =
        "{ "
            + "\"name\" : \""
            + sample.getName()
            + "\", "
            + "\"release\" : \""
            + dateTimeFormatter.format(sample.getRelease().atOffset(ZoneOffset.UTC))
            + "\""
            + "}";

    when(bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
            any(Sample.class), any(String.class)))
        .thenReturn(sampleWithWebinId);
    when(bioSamplesWebinAuthenticationService.getWebinSubmissionAccount(any(String.class)))
        .thenReturn(ResponseEntity.ok(submissionAccount));
    when(sampleService.persistSample(any(Sample.class), eq(AuthorizationProvider.WEBIN), eq(false)))
        .thenReturn(sampleWithWebinId);
    when(taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(
            any(Sample.class), eq(true)))
        .thenReturn(sampleWithWebinId);
    when(schemaValidationService.validate(any(Sample.class))).thenReturn("BSDC00001");
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.WEBIN, "WEBIN-12345", Collections.emptyList())));
    when(accessControlService.extractToken(null)).thenReturn(Optional.empty());

    this.mockMvc
        .perform(
            post("/biosamples/samples")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleToSubmit)
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "post-sample-2",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  /**
   * Generate the snippets for Sample submission to BioSamples with external references
   *
   * @throws Exception
   */
  @Test
  public void postSampleWithExternalReferences() throws Exception {
    Sample sample = this.faker.getExampleSample();
    Sample sampleWithDomain = this.faker.getExampleSampleWithExternalReferences();

    String sampleToSubmit =
        "{ "
            + "\"name\" : \""
            + sample.getName()
            + "\", "
            + "\"release\" : \""
            + dateTimeFormatter.format(sample.getRelease().atOffset(ZoneOffset.UTC))
            + "\", "
            + "\"domain\" : \"self.ExampleDomain\", "
            + "\"externalReferences\" : [ { "
            + "    \"url\" : \"https://www.ebi.ac.uk/ena/data/view/SAMEA00001\" "
            + "  } ]"
            + "}";

    when(aapService.handleSampleDomain(any(Sample.class))).thenReturn(sampleWithDomain);
    when(sampleService.persistSample(any(Sample.class), eq(AuthorizationProvider.AAP), eq(false)))
        .thenReturn(sampleWithDomain);
    when(schemaValidationService.validate(any(Sample.class))).thenReturn("BSDC00001");
    when(taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(
            any(Sample.class), eq(false)))
        .thenReturn(sampleWithDomain);
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.AAP, "user", Collections.emptyList())));
    when(accessControlService.extractToken(null)).thenReturn(Optional.empty());

    this.mockMvc
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
  public void putStructuredData() throws Exception {
    StructuredData structuredData = faker.getExampleStructuredData();
    when(structuredDataService.saveStructuredData(eq(structuredData))).thenReturn(structuredData);
    when(structuredDataService.getStructuredData(eq(structuredData.getAccession())))
        .thenReturn(Optional.of(structuredData));
    doNothing().when(aapService).handleStructuredDataDomain(eq(structuredData));
    when(aapService.isWriteSuperUser()).thenReturn(true);
    when(aapService.isIntegrationTestUser()).thenReturn(false);
    doNothing().when(aapService).checkSampleAccessibility(isA(Sample.class));
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.AAP, "user", Collections.emptyList())));

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
  public void postToGenerateAccession() throws Exception {
    Sample sample = this.faker.getExampleSample();
    Sample sampleWithDomain = this.faker.getExampleSampleWithDomain();
    Instant release =
        Instant.ofEpochSecond(
            LocalDateTime.now(ZoneOffset.UTC).plusYears(100).toEpochSecond(ZoneOffset.UTC));
    Sample sampleWithUpdatedDate =
        Sample.Builder.fromSample(sampleWithDomain).withRelease(release).build();

    String sampleToSubmit =
        "{ "
            + "\"name\" : \""
            + sample.getName()
            + "\", "
            + "\"update\" : \""
            + dateTimeFormatter.format(sample.getUpdate().atOffset(ZoneOffset.UTC))
            + "\", "
            + "\"domain\" : \"self.ExampleDomain\" "
            + "}";

    when(aapService.handleSampleDomain(any(Sample.class))).thenReturn(sampleWithUpdatedDate);
    when(sampleService.buildPrivateSample(any(Sample.class))).thenReturn(sampleWithUpdatedDate);
    when(sampleService.persistSample(any(Sample.class), eq(AuthorizationProvider.AAP), eq(false)))
        .thenReturn(sampleWithUpdatedDate);
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.AAP, "user-12345", Collections.emptyList())));
    when(accessControlService.extractToken(null)).thenReturn(Optional.empty());

    this.mockMvc
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

  @Test
  public void postToGenerateAccessionWithWebinAuthentication() throws Exception {
    Sample sample = this.faker.getExampleSample();
    Sample sampleWithWebinId = this.faker.getExampleSampleWithWebinId();
    Instant release =
        Instant.ofEpochSecond(
            LocalDateTime.now(ZoneOffset.UTC).plusYears(100).toEpochSecond(ZoneOffset.UTC));
    Sample sampleWithUpdatedDate =
        Sample.Builder.fromSample(sampleWithWebinId).withRelease(release).build();

    String sampleToSubmit =
        "{ "
            + "\"name\" : \""
            + sample.getName()
            + "\", "
            + "\"update\" : \""
            + dateTimeFormatter.format(sample.getUpdate().atOffset(ZoneOffset.UTC))
            + "\" "
            //                "\"release\" : \""
            // +dateTimeFormatter.format(sample.getRelease().atOffset(ZoneOffset.UTC)) +
            // "\", " +
            + "}";

    SubmissionAccount submissionAccount = new SubmissionAccount();
    submissionAccount.setId("WEBIN-12345");

    when(bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
            any(Sample.class), any(String.class)))
        .thenReturn(sampleWithWebinId);
    when(bioSamplesWebinAuthenticationService.getWebinSubmissionAccount(any(String.class)))
        .thenReturn(ResponseEntity.ok(submissionAccount));
    when(sampleService.buildPrivateSample(any(Sample.class))).thenReturn(sampleWithUpdatedDate);
    when(sampleService.persistSample(any(Sample.class), eq(AuthorizationProvider.WEBIN), eq(false)))
        .thenReturn(sampleWithUpdatedDate);
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.WEBIN, "WEBIN-12345", Collections.emptyList())));
    when(accessControlService.extractToken(null)).thenReturn(Optional.empty());

    this.mockMvc
        .perform(
            post("/biosamples/samples/accession")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleToSubmit)
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "accession-sample-2",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  /** Accessioning service to generate accession */
  @Test
  public void post_for_accessioning_with_accession_should_get_error() throws Exception {
    Sample sample = this.faker.getExampleSample();
    Sample sampleWithDomain = this.faker.getExampleSampleWithDomain();

    String sampleToSubmit =
        "{ "
            + "\"accession\" : \""
            + "FakeAccession"
            + "\", "
            + "\"name\" : \""
            + sample.getName()
            + "\", "
            + "\"update\" : \""
            + dateTimeFormatter.format(sample.getUpdate().atOffset(ZoneOffset.UTC))
            + "\", "
            + "\"release\" : \""
            + dateTimeFormatter.format(sample.getRelease().atOffset(ZoneOffset.UTC))
            + "\", "
            + "\"domain\" : \"self.ExampleDomain\" "
            + "}";

    when(aapService.handleSampleDomain(any(Sample.class))).thenReturn(sampleWithDomain);
    when(sampleService.persistSample(any(Sample.class), eq(AuthorizationProvider.WEBIN), eq(false)))
        .thenReturn(sampleWithDomain);

    this.mockMvc
        .perform(
            post("/biosamples/samples/accession")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleToSubmit)
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is4xxClientError());
  }

  /** validation service to validate basic fields */
  @Test
  public void post_for_validation() throws Exception {
    Sample sample = this.faker.getExampleSample();
    String sampleToSubmit =
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

    this.mockMvc
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
  public void post_for_recommendation() throws Exception {
    Sample sample = this.faker.getExampleSample();
    String sampleToSubmit =
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

    this.mockMvc
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
  public void post_for_certification() throws Exception {
    Sample sample = this.faker.getExampleSampleWithDomain();
    Attribute attribute = Attribute.build("Organism", "Homo Sapiens");

    sample =
        Sample.Builder.fromSample(sample)
            .withAttributes(Collections.unmodifiableList(Arrays.asList(attribute)))
            .build();

    when(sampleService.fetch(eq(sample.getAccession()), eq(Optional.empty()), any(String.class)))
        .thenReturn(Optional.of(sample));
    when(aapService.handleSampleDomain(sample)).thenReturn(sample);
    when(aapService.isWriteSuperUser()).thenReturn(true);
    when(aapService.isIntegrationTestUser()).thenReturn(false);
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.AAP, "user-12345", Collections.emptyList())));
    when(accessControlService.extractToken(null)).thenReturn(Optional.empty());

    List<Certificate> certificates = new java.util.ArrayList<>();

    certificates.add(
        Certificate.build(
            "biosamples-minimal", "0.0.1", "schemas/certification/biosamples-minimal.json"));
    when(certifyService.certify(new ObjectMapper().writeValueAsString(sample), true))
        .thenReturn(certificates);

    certificates.add(
        Certificate.build(
            "biosamples-minimal", "0.0.1", "schemas/certification/biosamples-minimal.json"));

    when(sampleService.persistSample(any(Sample.class), eq(AuthorizationProvider.AAP), eq(false)))
        .thenReturn(Sample.Builder.fromSample(sample).withCertificates(certificates).build());
    doNothing().when(aapService).checkSampleAccessibility(isA(Sample.class));

    this.mockMvc
        .perform(
            put("/biosamples/samples/" + sample.getAccession() + "/certify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(sample))
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "certify-sample",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  @Test
  public void post_for_certification_with_webin_authentication() throws Exception {
    Sample sampleWithWebinId = this.faker.getExampleSampleWithWebinId();
    Attribute attribute = Attribute.build("Organism", "Homo Sapiens");

    sampleWithWebinId =
        Sample.Builder.fromSample(sampleWithWebinId)
            .withAttributes(Collections.unmodifiableList(Arrays.asList(attribute)))
            .build();

    SubmissionAccount submissionAccount = new SubmissionAccount();
    submissionAccount.setId("WEBIN-12345");

    when(bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
            any(Sample.class), any(String.class)))
        .thenReturn(sampleWithWebinId);
    when(bioSamplesWebinAuthenticationService.getWebinSubmissionAccount(any(String.class)))
        .thenReturn(ResponseEntity.ok(submissionAccount));
    when(sampleService.fetch(
            eq(sampleWithWebinId.getAccession()), eq(Optional.empty()), any(String.class)))
        .thenReturn(Optional.of(sampleWithWebinId));
    when(aapService.handleSampleDomain(sampleWithWebinId)).thenReturn(sampleWithWebinId);
    when(aapService.isWriteSuperUser()).thenReturn(true);
    when(aapService.isIntegrationTestUser()).thenReturn(false);

    List<Certificate> certificates = new java.util.ArrayList<>();
    certificates.add(
        Certificate.build(
            "biosamples-minimal", "0.0.1", "schemas/certification/biosamples-minimal.json"));

    when(certifyService.certify(new ObjectMapper().writeValueAsString(sampleWithWebinId), true))
        .thenReturn(certificates);
    when(sampleService.persistSample(
            eq(sampleWithWebinId), eq(AuthorizationProvider.WEBIN), eq(false)))
        .thenReturn(
            Sample.Builder.fromSample(sampleWithWebinId).withCertificates(certificates).build());
    doNothing().when(aapService).checkSampleAccessibility(isA(Sample.class));
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.WEBIN, "WEBIN-12345", Collections.emptyList())));
    when(accessControlService.extractToken(null)).thenReturn(Optional.empty());

    this.mockMvc
        .perform(
            put("/biosamples/samples/" + sampleWithWebinId.getAccession() + "/certify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(sampleWithWebinId))
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "certify-sample-2",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  @Test
  public void post_for_checking_compliance() throws Exception {
    Sample sample = this.faker.getExampleSampleWithDomain();
    Attribute attribute = Attribute.build("INSDC status", "live");

    sample =
        Sample.Builder.fromSample(sample)
            .withAttributes(Collections.unmodifiableList(Arrays.asList(attribute)))
            .build();

    BioSamplesCertificationComplainceResult bioSamplesCertificationComplainceResult =
        new BioSamplesCertificationComplainceResult();

    Suggestion suggestion = new Suggestion();
    suggestion.setCharacteristic(new String[] {"organism", "species"});
    suggestion.setMandatory(true);
    suggestion.setComment("Either Organism or Species must be present in sample");

    CurationResult curationResult = new CurationResult("INSDC status", "live", "public");

    bioSamplesCertificationComplainceResult.add(
        new uk.ac.ebi.biosamples.model.certification.Certificate(
            new SampleDocument("SAMFAKE123456", new ObjectMapper().writeValueAsString(sample)),
            new ArrayList<>(),
            new Checklist(
                "ncbi-candidate-schema",
                "0.0.1",
                "schemas/certification/ncbi-candidate-schema.json",
                false)));

    List<CurationResult> curationResults = new ArrayList<>();
    curationResults.add(curationResult);

    bioSamplesCertificationComplainceResult.add(
        new uk.ac.ebi.biosamples.model.certification.Certificate(
            new SampleDocument("SAMFAKE123456", new ObjectMapper().writeValueAsString(sample)),
            curationResults,
            new Checklist(
                "biosamples-basic",
                "0.0.1",
                "schemas/certification/biosamples-basic.json",
                false)));

    List<Suggestion> suggestions = new ArrayList<>();
    suggestions.add(suggestion);

    bioSamplesCertificationComplainceResult.add(
        new Recommendation("biosamples-minimal-0.0.1", suggestions));

    when(certifyService.recordResult(new ObjectMapper().writeValueAsString(sample), true))
        .thenReturn(bioSamplesCertificationComplainceResult);

    this.mockMvc
        .perform(
            post("/biosamples/samples/checkCompliance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(sample))
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "check-compliance",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  @Test
  public void putSample() throws Exception {

    Sample sampleWithDomain = this.faker.getExampleSampleWithDomain();

    when(sampleService.fetch(
            eq(sampleWithDomain.getAccession()), eq(Optional.empty()), any(String.class)))
        .thenReturn(Optional.of(sampleWithDomain));
    when(sampleService.persistSample(
            eq(sampleWithDomain), eq(AuthorizationProvider.AAP), eq(false)))
        .thenReturn(sampleWithDomain);
    when(aapService.handleSampleDomain(sampleWithDomain)).thenReturn(sampleWithDomain);
    when(aapService.isWriteSuperUser()).thenReturn(true);
    when(aapService.isIntegrationTestUser()).thenReturn(false);
    doNothing().when(aapService).checkSampleAccessibility(isA(Sample.class));
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.AAP, "user", Collections.emptyList())));

    this.mockMvc
        .perform(
            put("/biosamples/samples/" + sampleWithDomain.getAccession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(sampleWithDomain))
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "put-sample", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
  }

  @Test
  public void putSampleWithWebinAuthentication() throws Exception {

    Sample sampleWithWebinId = this.faker.getExampleSampleWithWebinId();
    SubmissionAccount submissionAccount = new SubmissionAccount();

    submissionAccount.setId("WEBIN-12345");

    when(bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
            any(Sample.class), any(String.class)))
        .thenReturn(sampleWithWebinId);
    when(bioSamplesWebinAuthenticationService.getWebinSubmissionAccount(any(String.class)))
        .thenReturn(ResponseEntity.ok(submissionAccount));
    when(sampleService.fetch(
            eq(sampleWithWebinId.getAccession()), eq(Optional.empty()), any(String.class)))
        .thenReturn(Optional.of(sampleWithWebinId));
    when(sampleService.persistSample(
            eq(sampleWithWebinId), eq(AuthorizationProvider.WEBIN), eq(false)))
        .thenReturn(sampleWithWebinId);

    when(taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(
            any(Sample.class), eq(true)))
        .thenReturn(sampleWithWebinId);
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.WEBIN, "user", Collections.emptyList())));

    this.mockMvc
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
  public void putSampleWithRelationships() throws Exception {
    Sample sampleWithDomain = this.faker.getExampleSampleWithRelationships();

    when(sampleService.fetch(
            eq(sampleWithDomain.getAccession()), eq(Optional.empty()), any(String.class)))
        .thenReturn(Optional.of(sampleWithDomain));
    when(sampleService.persistSample(
            eq(sampleWithDomain), eq(AuthorizationProvider.AAP), eq(false)))
        .thenReturn(sampleWithDomain);
    when(aapService.handleSampleDomain(sampleWithDomain)).thenReturn(sampleWithDomain);
    when(aapService.isWriteSuperUser()).thenReturn(true);
    when(aapService.isIntegrationTestUser()).thenReturn(false);
    doNothing().when(aapService).checkSampleAccessibility(isA(Sample.class));
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.AAP, "user", Collections.emptyList())));

    this.mockMvc
        .perform(
            put("/biosamples/samples/" + sampleWithDomain.getAccession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(sampleWithDomain))
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "put-sample-with-relationships",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  @Test
  public void postCurationLink() throws Exception {
    CurationLink curationLink = this.faker.getExampleCurationLink();
    when(aapService.handleCurationLinkDomain(eq(curationLink))).thenReturn(curationLink);
    when(curationPersistService.store(curationLink)).thenReturn(curationLink);
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.AAP, "user", Collections.emptyList())));
    when(accessControlService.extractToken(null)).thenReturn(Optional.empty());

    this.mockMvc
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
  public void postCurationLinkWithWebinAuthentication() throws Exception {
    CurationLink curationLink = this.faker.getExampleCurationLinkWithWebinId();
    SubmissionAccount submissionAccount = new SubmissionAccount();
    submissionAccount.setId("WEBIN-12345");

    when(bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
            eq(curationLink), eq("WEBIN-12345")))
        .thenReturn(curationLink);
    when(curationPersistService.store(curationLink)).thenReturn(curationLink);
    when(bioSamplesWebinAuthenticationService.getWebinSubmissionAccount(any(String.class)))
        .thenReturn(ResponseEntity.ok(submissionAccount));
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.WEBIN, "WEBIN-12345", Collections.emptyList())));
    when(accessControlService.extractToken(null)).thenReturn(Optional.empty());

    this.mockMvc
        .perform(
            post("/biosamples/samples/{accession}/curationlinks", curationLink.getSample())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(curationLink))
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "post-curation-2",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  @Test
  public void getSample() throws Exception {
    Sample sample = faker.getExampleSampleBuilder().withDomain(faker.getExampleDomain()).build();
    when(sampleService.fetch(sample.getAccession(), Optional.empty(), null))
        .thenReturn(Optional.of(sample));
    doNothing().when(aapService).checkSampleAccessibility(isA(Sample.class));
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
    Page<Curation> curationPage =
        new PageImpl<>(
            Collections.singletonList(this.faker.getExampleCurationLink().getCuration()),
            getDefaultPageable(),
            100);
    when(curationReadService.getPage(isA(Pageable.class))).thenReturn(curationPage);

    this.mockMvc
        .perform(get("/biosamples/curations").accept(MediaTypes.HAL_JSON))
        .andExpect(status().is2xxSuccessful())
        .andDo(document("get-curations"));
  }

  @Test
  public void getSampleCurationLinks() throws Exception {
    Page<CurationLink> curationLinkPage =
        new PageImpl<>(
            Collections.singletonList(this.faker.getExampleCurationLink()),
            getDefaultPageable(),
            100);
    String sampleAccession = curationLinkPage.getContent().get(0).getSample();

    when(curationReadService.getCurationLinksForSample(eq(sampleAccession), isA(Pageable.class)))
        .thenReturn(curationLinkPage);

    this.mockMvc
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

  private Sort.Order parseSort(String sort) {
    if (sort.endsWith(",desc")) {
      return new Sort.Order(Sort.Direction.DESC, sort.substring(0, sort.length() - 5));
    } else if (sort.endsWith(",asc")) {
      return new Sort.Order(Sort.Direction.ASC, sort.substring(0, sort.length() - 4));
    } else {
      return new Sort.Order(null, sort);
    }
  }

  private String serialize(Object obj) throws JsonProcessingException {
    return mapper.writeValueAsString(obj);
  }
}
