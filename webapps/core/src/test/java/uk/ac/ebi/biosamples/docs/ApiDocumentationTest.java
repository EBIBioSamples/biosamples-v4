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
package uk.ac.ebi.biosamples.docs;

import static org.mockito.Matchers.*;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.model.Certificate;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.certification.*;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.*;
import uk.ac.ebi.biosamples.service.certification.CertifyService;
import uk.ac.ebi.biosamples.service.certification.Identifier;

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

  @MockBean private SampleService sampleService;

  @MockBean private CertifyService certifyService;

  @MockBean CurationPersistService curationPersistService;

  @MockBean CurationReadService curationReadService;

  @MockBean private BioSamplesAapService aapService;

  @MockBean private Identifier identifier;

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
    Page<Sample> samplePage =
        new PageImpl<>(Collections.singletonList(fakeSample), getDefaultPageable(), 100);
    when(samplePageService.getSamplesByText(
            any(String.class),
            anyCollectionOf(Filter.class),
            anyCollectionOf(String.class),
            isA(Pageable.class),
            any(String.class)))
        .thenReturn(samplePage);
    this.mockMvc
        .perform(get("/biosamples/samples").accept(MediaTypes.HAL_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "get-samples",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestParameters(
                    parameterWithName("page").description("The page to retrieve").optional(),
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
            "Sample without minimum information", null, null, null, null, null, null, null, null, null);

    when(aapService.handleSampleDomain(any(Sample.class))).thenReturn(wrongSample);
    when(sampleService.store(wrongSample, false)).thenCallRealMethod();
    when(sampleService.store(wrongSample, false)).thenCallRealMethod();
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
    when(sampleService.store(any(Sample.class), eq(false))).thenReturn(sampleWithDomain);

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
    when(sampleService.store(any(Sample.class), eq(false))).thenReturn(sampleWithDomain);

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
   * Generate the snippets for Sample submission with structured data to BioSamples
   *
   * @throws Exception
   */
  @Test
  public void patchSample() throws Exception {
    Sample sampleWithDomainAndData = this.faker.getExampleSampleWithStructuredData();

    when(sampleService.fetch(
            eq(sampleWithDomainAndData.getAccession()), eq(Optional.empty()), any(String.class)))
        .thenReturn(Optional.of(sampleWithDomainAndData));
    when(aapService.handleStructuredDataDomain(sampleWithDomainAndData))
        .thenReturn(sampleWithDomainAndData);
    when(sampleService.storeSampleStructuredData(eq(sampleWithDomainAndData)))
        .thenReturn(sampleWithDomainAndData);
    when(aapService.isWriteSuperUser()).thenReturn(true);
    when(aapService.isIntegrationTestUser()).thenReturn(false);
    doNothing().when(aapService).checkAccessible(isA(Sample.class));

    this.mockMvc
        .perform(
            patch(
                    "/biosamples/samples/"
                        + sampleWithDomainAndData.getAccession()
                        + "?structuredData=true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(sampleWithDomainAndData))
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "patch-sample",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  @Test
  public void patchSample2() throws Exception {
    Sample sampleWithDomainAndData = this.faker.getExampleSampleWithStructuredData2();

    when(sampleService.fetch(
            eq(sampleWithDomainAndData.getAccession()), eq(Optional.empty()), any(String.class)))
        .thenReturn(Optional.of(sampleWithDomainAndData));
    when(aapService.handleStructuredDataDomain(sampleWithDomainAndData))
        .thenReturn(sampleWithDomainAndData);
    when(sampleService.storeSampleStructuredData(eq(sampleWithDomainAndData)))
        .thenReturn(sampleWithDomainAndData);
    when(aapService.isWriteSuperUser()).thenReturn(true);
    when(aapService.isIntegrationTestUser()).thenReturn(false);
    doNothing().when(aapService).checkAccessible(isA(Sample.class));

    this.mockMvc
        .perform(
            patch(
                    "/biosamples/samples/"
                        + sampleWithDomainAndData.getAccession()
                        + "?structuredData=true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(sampleWithDomainAndData))
                .header("Authorization", "Bearer $TOKEN"))
        .andExpect(status().is2xxSuccessful())
        .andDo(
            document(
                "patch-sample-2",
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
            +
            //                "\"release\" : \""
            // +dateTimeFormatter.format(sample.getRelease().atOffset(ZoneOffset.UTC)) +
            // "\", " +
            "\"domain\" : \"self.ExampleDomain\" "
            + "}";

    when(aapService.handleSampleDomain(any(Sample.class))).thenReturn(sampleWithUpdatedDate);
    when(sampleService.store(any(Sample.class), eq(false))).thenReturn(sampleWithUpdatedDate);

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
    when(sampleService.store(any(Sample.class), eq(false))).thenReturn(sampleWithDomain);

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
    Sample sampleWithDomain = this.faker.getExampleSampleWithDomain();

    String sampleToSubmit =
        "{ "
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

    this.mockMvc
        .perform(
            post("/biosamples/samples/validate")
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

  /** Recommendation service to suggest sample attributes */
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
    when(certifyService.certify(new ObjectMapper().writeValueAsString(sample), true))
        .thenReturn(
            List.of(
                Certificate.build(
                    "biosamples-minimal",
                    "0.0.1",
                    "schemas/certification/biosamples-minimal.json")));
    when(sampleService.store(eq(sample), eq(false)))
        .thenReturn(
            Sample.Builder.fromSample(sample)
                .withCertificates(
                    List.of(
                        Certificate.build(
                            "biosamples-minimal",
                            "0.0.1",
                            "schemas/certification/biosamples-minimal.json")))
                .build());
    doNothing().when(aapService).checkAccessible(isA(Sample.class));

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
            List.of(),
            new Checklist(
                "ncbi-candidate-schema",
                "0.0.1",
                "schemas/certification/ncbi-candidate-schema.json",
                false)));
    bioSamplesCertificationComplainceResult.add(
        new uk.ac.ebi.biosamples.model.certification.Certificate(
            new SampleDocument("SAMFAKE123456", new ObjectMapper().writeValueAsString(sample)),
            List.of(curationResult),
            new Checklist(
                "biosamples-basic",
                "0.0.1",
                "schemas/certification/biosamples-basic.json",
                false)));

    bioSamplesCertificationComplainceResult.add(
        new Recommendation("biosamples-minimal-0.0.1", List.of(suggestion)));

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
    when(sampleService.store(eq(sampleWithDomain), eq(false))).thenReturn(sampleWithDomain);
    when(aapService.handleSampleDomain(sampleWithDomain)).thenReturn(sampleWithDomain);
    when(aapService.isWriteSuperUser()).thenReturn(true);
    when(aapService.isIntegrationTestUser()).thenReturn(false);
    doNothing().when(aapService).checkAccessible(isA(Sample.class));

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
    when(sampleService.store(eq(sampleWithDomain), eq(false))).thenReturn(sampleWithDomain);
    when(aapService.handleSampleDomain(sampleWithDomain)).thenReturn(sampleWithDomain);
    when(aapService.isWriteSuperUser()).thenReturn(true);
    when(aapService.isIntegrationTestUser()).thenReturn(false);
    doNothing().when(aapService).checkAccessible(isA(Sample.class));

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
  public void getSample() throws Exception {
    Sample sample =
        this.faker.getExampleSampleBuilder().withDomain(this.faker.getExampleDomain()).build();
    when(sampleService.fetch(sample.getAccession(), Optional.empty(), null))
        .thenReturn(Optional.of(sample));
    doNothing().when(aapService).checkAccessible(isA(Sample.class));

    this.mockMvc
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
    return new PageRequest(0, 1, getDefaultSort());
  }

  private Sort getDefaultSort() {
    return new Sort(
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
