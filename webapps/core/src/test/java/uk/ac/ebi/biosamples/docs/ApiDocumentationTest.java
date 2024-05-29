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

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"spring.cloud.gcp.project-id=no_project"})
@AutoConfigureRestDocs
@TestPropertySource(properties = {"aap.domains.url = ''"})
@Ignore
public class ApiDocumentationTest {

  /*@Rule
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

  */
  /**
   * Generate the snippets for the API root
   *
   * @throws Exception
   */
  /*
  @Test
  public void getIndex() throws Exception {
    mockMvc
        .perform(get("/biosamples").accept(MediaTypes.HAL_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "get-index", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
  }

  */
  /**
   * Generate the snippets for the samples root page
   *
   * @throws Exception
   */
  /*
  @Test
  public void getSamples() throws Exception {
    final Sample fakeSample = faker.getExampleSample();
    when(samplePageService.getSamplesByText(
            nullable(String.class),
            anyList(),
            anySet(),
            nullable(String.class),
            any(Pageable.class),
            any()))
        .thenReturn(
            new PageImpl<>(Collections.singletonList(fakeSample), getDefaultPageable(), 100));

    when(samplePageService.getSamplesByText(
            nullable(String.class),
            anyList(),
            anySet(),
            nullable(String.class),
            nullable(String.class),
            anyInt(),
            any()))
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
                        .description(
                            "The page to retrieve. Not recommended to use for pagination of large number of results")
                        .optional(),
                    parameterWithName("text").description("Text to search").optional(),
                    parameterWithName("filter")
                        .description("List of filters to apply to search results")
                        .optional())));
  }

  */
  /**
   * Describe what's the minimal information necessary to submit a sample
   *
   * @throws Exception
   */
  /*
  @Test
  @Ignore
  public void postSampleMinimalInfo() throws Exception {
    final ObjectMapper jsonMapper = new ObjectMapper();
    final String wrongSampleSerialized = "{\"name\": \"Sample without minimum information\" }";
    final Sample wrongSample =
        Sample.build(
            "Sample without minimum information",
            null,
            null,
            null,
            null,
            Long.valueOf(9606),
            SampleStatus.PUBLIC,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    when(aapService.handleSampleDomain(any(Sample.class), any())).thenReturn(wrongSample);
    when(sampleService.persistSample(wrongSample, null, null, false)).thenCallRealMethod();
    when(sampleService.persistSample(wrongSample, null, null, false)).thenCallRealMethod();
    when(certifyService.certify(jsonMapper.writeValueAsString(wrongSample), true))
        .thenReturn(Collections.emptyList());

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

  */
  /**
   * Describes what's the error when curationLink minimal information is not provided
   *
   * @throws Exception
   */
  /*
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

  */
  /**
   * Generate the snippets for Sample submission to BioSamples
   *
   * @throws Exception
   */
  /*
  @Test
  public void postSample() throws Exception {
    final Sample sample = faker.getExampleSample();
    final Sample sampleWithDomain = faker.getExampleSampleWithDomain();

    final String sampleToSubmit =
        "{ "
            + "\"name\" : \""
            + sample.getName()
            + "\", "
            + "\"release\" : \""
            + dateTimeFormatter.format(sample.getRelease().atOffset(ZoneOffset.UTC))
            + "\", "
            + "\"domain\" : \"self.ExampleDomain\" "
            + "}";

    when(aapService.handleSampleDomain(any(Sample.class), any())).thenReturn(sampleWithDomain);
    when(sampleService.persistSample(
            any(Sample.class), eq(null), eq(AuthorizationProvider.AAP), eq(false)))
        .thenReturn(sampleWithDomain);
    when(schemaValidationService.validate(any(Sample.class))).thenReturn(sampleWithDomain);
    when(taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(
            any(Sample.class), eq(false)))
        .thenReturn(sampleWithDomain);
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.AAP, "user", Collections.emptyList())));
    when(accessControlService.extractToken(null)).thenReturn(Optional.empty());

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

  */
  /**
   * Generate the snippets for Sample submission to BioSamples
   *
   * @throws Exception
   */
  /*
  @Test
  public void postSampleWithWebinAuthentication() throws Exception {
    final Sample sample = faker.getExampleSample();
    final Sample sampleWithWebinId = faker.getExampleSampleWithWebinId();

    final String sampleToSubmit =
        "{ "
            + "\"name\" : \""
            + sample.getName()
            + "\", "
            + "\"release\" : \""
            + dateTimeFormatter.format(sample.getRelease().atOffset(ZoneOffset.UTC))
            + "\""
            + "}";

    when(bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
            any(Sample.class), any(String.class), eq(Optional.empty())))
        .thenReturn(sampleWithWebinId);
    when(sampleService.persistSample(
            any(Sample.class), eq(null), eq(AuthorizationProvider.WEBIN), eq(false)))
        .thenReturn(sampleWithWebinId);
    when(taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(
            any(Sample.class), eq(true)))
        .thenReturn(sampleWithWebinId);
    when(schemaValidationService.validate(any(Sample.class))).thenReturn(sampleWithWebinId);
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.WEBIN, "WEBIN-12345", Collections.emptyList())));
    when(accessControlService.extractToken(null)).thenReturn(Optional.empty());

    mockMvc
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

  */
  /**
   * Generate the snippets for Sample submission to BioSamples with external references
   *
   * @throws Exception
   */
  /*
  @Test
  public void postSampleWithExternalReferences() throws Exception {
    final Sample sample = faker.getExampleSample();
    final Sample sampleWithDomain = faker.getExampleSampleWithExternalReferences();

    final String sampleToSubmit =
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

    when(aapService.handleSampleDomain(any(Sample.class), any())).thenReturn(sampleWithDomain);
    when(sampleService.persistSample(
            any(Sample.class), eq(null), eq(AuthorizationProvider.AAP), eq(false)))
        .thenReturn(sampleWithDomain);
    when(schemaValidationService.validate(any(Sample.class))).thenReturn(sampleWithDomain);
    when(taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(
            any(Sample.class), eq(false)))
        .thenReturn(sampleWithDomain);
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.AAP, "user", Collections.emptyList())));
    when(accessControlService.extractToken(null)).thenReturn(Optional.empty());

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

  */
  /**
   * Generate the snippets for submitting structured data to BioSamples
   *
   * @throws Exception
   */
  /*
  @Test
  public void putStructuredData() throws Exception {
    final StructuredData structuredData = faker.getExampleStructuredData();
    when(structuredDataService.saveStructuredData(eq(structuredData))).thenReturn(structuredData);
    when(structuredDataService.getStructuredData(eq(structuredData.getAccession())))
        .thenReturn(Optional.of(structuredData));
    doNothing().when(aapService).handleStructuredDataDomain(eq(structuredData));
    when(aapService.isWriteSuperUser()).thenReturn(true);
    when(aapService.isIntegrationTestUser()).thenReturn(false);
    doNothing().when(aapService).isSampleAccessible(isA(Sample.class));
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

  */
  /** Accessioning service to generate accession */
  /*
  @Test
  public void postToGenerateAccession() throws Exception {
    final Sample sample = faker.getExampleSample();
    final Sample sampleWithDomain = faker.getExampleSampleWithDomain();
    final Instant release =
        Instant.ofEpochSecond(
            LocalDateTime.now(ZoneOffset.UTC).plusYears(100).toEpochSecond(ZoneOffset.UTC));
    final Sample sampleWithUpdatedDate =
        Sample.Builder.fromSample(sampleWithDomain).withRelease(release).build();

    final String sampleToSubmit =
        "{ "
            + "\"name\" : \""
            + sample.getName()
            + "\", "
            + "\"update\" : \""
            + dateTimeFormatter.format(sample.getUpdate().atOffset(ZoneOffset.UTC))
            + "\", "
            + "\"domain\" : \"self.ExampleDomain\" "
            + "}";

    when(aapService.handleSampleDomain(any(Sample.class), any())).thenReturn(sampleWithUpdatedDate);
    when(sampleService.buildPrivateSample(any(Sample.class))).thenReturn(sampleWithUpdatedDate);
    when(sampleService.persistSample(
            any(Sample.class), eq(null), eq(AuthorizationProvider.AAP), eq(false)))
        .thenReturn(sampleWithUpdatedDate);
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.AAP, "user-12345", Collections.emptyList())));
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

  @Test
  public void postToGenerateAccessionWithWebinAuthentication() throws Exception {
    final Sample sample = faker.getExampleSample();
    final Sample sampleWithWebinId = faker.getExampleSampleWithWebinId();
    final Instant release =
        Instant.ofEpochSecond(
            LocalDateTime.now(ZoneOffset.UTC).plusYears(100).toEpochSecond(ZoneOffset.UTC));
    final Sample sampleWithUpdatedDate =
        Sample.Builder.fromSample(sampleWithWebinId).withRelease(release).build();

    final String sampleToSubmit =
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

    when(bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
            any(Sample.class), any(String.class), eq(Optional.empty())))
        .thenReturn(sampleWithWebinId);
    when(sampleService.buildPrivateSample(any(Sample.class))).thenReturn(sampleWithUpdatedDate);
    when(sampleService.persistSample(
            any(Sample.class), eq(null), eq(AuthorizationProvider.WEBIN), eq(false)))
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
                "accession-sample-2",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  */
  /** Accessioning service to generate accession */
  /*
  @Test
  public void post_for_accessioning_with_accession_should_get_error() throws Exception {
    final Sample sample = faker.getExampleSample();
    final Sample sampleWithDomain = faker.getExampleSampleWithDomain();

    final String sampleToSubmit =
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

    when(aapService.handleSampleDomain(any(Sample.class), any())).thenReturn(sampleWithDomain);
    when(sampleService.persistSample(
            any(Sample.class), eq(null), eq(AuthorizationProvider.WEBIN), eq(false)))
        .thenReturn(sampleWithDomain);

    mockMvc
        .perform(
            post("/biosamples/samples/accession")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleToSubmit)
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is4xxClientError());
  }

  */
  /** validation service to validate basic fields */
  /*
  @Test
  public void post_for_validation() throws Exception {
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

  */
  /** Recommendation service to suggest sample attributes */
  /*
  @Test
  public void post_for_recommendation() throws Exception {
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
  public void putSample() throws Exception {

    final Sample sampleWithDomain = faker.getExampleSampleWithDomain();

    when(sampleService.fetch(eq(sampleWithDomain.getAccession()), eq(Optional.empty())))
        .thenReturn(Optional.of(sampleWithDomain));
    when(sampleService.persistSample(
            eq(sampleWithDomain), eq(sampleWithDomain), eq(AuthorizationProvider.AAP), eq(false)))
        .thenReturn(sampleWithDomain);
    when(aapService.handleSampleDomain(sampleWithDomain, Optional.of(sampleWithDomain)))
        .thenReturn(sampleWithDomain);
    when(aapService.isWriteSuperUser()).thenReturn(true);
    when(aapService.isIntegrationTestUser()).thenReturn(false);
    doNothing().when(aapService).isSampleAccessible(isA(Sample.class));
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.AAP, "user", Collections.emptyList())));

    mockMvc
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

    final Sample sampleWithWebinId = faker.getExampleSampleWithWebinId();

    when(bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
            any(Sample.class), any(String.class), eq(Optional.of(sampleWithWebinId))))
        .thenReturn(sampleWithWebinId);
    when(sampleService.fetch(eq(sampleWithWebinId.getAccession()), eq(Optional.empty())))
        .thenReturn(Optional.of(sampleWithWebinId));
    when(sampleService.isNotExistingAccession(sampleWithWebinId.getAccession())).thenReturn(false);
    when(sampleService.persistSample(
            eq(sampleWithWebinId),
            eq(sampleWithWebinId),
            eq(AuthorizationProvider.WEBIN),
            eq(false)))
        .thenReturn(sampleWithWebinId);

    when(taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(
            any(Sample.class), eq(true)))
        .thenReturn(sampleWithWebinId);
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.WEBIN, "user", Collections.emptyList())));

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

  */
  /**
   * Generate the snippets for Sample submission to BioSamples with relationships
   *
   * @throws Exception
   */
  /*
  @Test
  public void putSampleWithRelationships() throws Exception {
    final Sample sampleWithDomain = faker.getExampleSampleWithRelationships();

    when(sampleService.fetch(eq(sampleWithDomain.getAccession()), eq(Optional.empty())))
        .thenReturn(Optional.of(sampleWithDomain));
    when(sampleService.persistSample(
            eq(sampleWithDomain), eq(sampleWithDomain), eq(AuthorizationProvider.AAP), eq(false)))
        .thenReturn(sampleWithDomain);
    when(aapService.handleSampleDomain(sampleWithDomain, Optional.of(sampleWithDomain)))
        .thenReturn(sampleWithDomain);
    when(aapService.isWriteSuperUser()).thenReturn(true);
    when(aapService.isIntegrationTestUser()).thenReturn(false);
    doNothing().when(aapService).isSampleAccessible(isA(Sample.class));
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.AAP, "user", Collections.emptyList())));

    mockMvc
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
  @Ignore
  public void postCurationLink() throws Exception {
    final CurationLink curationLink = faker.getExampleCurationLink();
    when(aapService.handleCurationLinkDomain(eq(curationLink))).thenReturn(curationLink);
    when(curationPersistService.store(curationLink)).thenReturn(curationLink);
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.AAP, "user", Collections.emptyList())));
    when(accessControlService.extractToken(null)).thenReturn(Optional.empty());

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
  @Ignore
  public void postCurationLinkWithWebinAuthentication() throws Exception {
    final CurationLink curationLink = faker.getExampleCurationLinkWithWebinId();

    when(bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
            eq(curationLink), eq("WEBIN-12345")))
        .thenReturn(curationLink);
    when(curationPersistService.store(curationLink)).thenReturn(curationLink);
    when(accessControlService.extractToken(anyString()))
        .thenReturn(
            Optional.of(
                new AuthToken(
                    "RS256", AuthorizationProvider.WEBIN, "WEBIN-12345", Collections.emptyList())));
    // when(accessControlService.extractToken(null)).thenReturn(Optional.empty());

    mockMvc
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
    final Sample sample =
        faker.getExampleSampleBuilder().withDomain(faker.getExampleDomain()).build();
    when(sampleService.fetch(sample.getAccession(), Optional.empty()))
        .thenReturn(Optional.of(sample));
    doNothing().when(aapService).isSampleAccessible(isA(Sample.class));
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
  }*/
}
