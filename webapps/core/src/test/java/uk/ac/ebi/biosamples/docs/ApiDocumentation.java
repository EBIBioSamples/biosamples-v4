package uk.ac.ebi.biosamples.docs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.CurationPersistService;
import uk.ac.ebi.biosamples.service.SamplePageService;
import uk.ac.ebi.biosamples.service.SampleService;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApiDocumentation {

    @Rule
    public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("target/generated-snippets");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private SamplePageService samplePageService;

    @MockBean
    private SampleService sampleService;

    @MockBean
    CurationPersistService curationPersistService;

    @MockBean
    private BioSamplesAapService aapService;

    private DocumentationHelper faker;

    private MockMvc mockMvc;

    private Traverson traverson;

//    @LocalServerPort
//    int port;


    @Before
    public void setUp() {
        this.faker = new DocumentationHelper();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)

                .apply(documentationConfiguration(this.restDocumentation).uris()
                    .withScheme("https")
                    .withHost("www.ebi.ac.uk")
                    .withPort(443)
                )
                .defaultRequest(get("/").contextPath("/biosamples"))
                .build();
    }

    /**
     * Generate the snippets for the API root
     * @throws Exception
     */
    @Test
    public void apiRoot() throws Exception {
        this.mockMvc.perform(get("/biosamples").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(document("index", links(halLinks(),
                        linkWithRel("samples").description("Link to all the sample resources"),
                        linkWithRel("curations").description("Link to all the curation resources")
                )));

    }

    /**
     * Generate the snippets for the samples root page
     * @throws Exception
     */
    @Test
    @Ignore
    public void samplesRoot() throws Exception {
        Page<Sample> samplePage = new PageImpl<>(this.faker.generateRandomSamples(100), getDefaultPageable(), 100);
        when(samplePageService.getSamplesByText(any(String.class), anyCollectionOf(Filter.class), anyCollectionOf(String.class), eq(getDefaultPageable())))
                .thenReturn(samplePage);
        this.mockMvc.perform(get("/biosamples/samples").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(document("samples"));
    }

    @Test
    @Ignore
    public void sampleSubmissionMinimumInformation() throws Exception {
        String wrongSampleSerialized = "{\"name\": \"Sample without minimum information\"," +
                " \"accession\": \"SAMEA123123123\"}";

        this.mockMvc.perform(
                post("/biosamples/samples")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(wrongSampleSerialized)
                    .header("Authorization", "Bearer $TOKEN"))
                .andExpect(status().is4xxClientError())
                .andDo(document("sample-minimal-information", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
    }

    /**
     * Generate the snippets for Sample submission to BioSamples
     * @throws Exception
     */
    @Test
    @Ignore
    public void submitSample() throws Exception {
        Sample sample = this.faker.generateRandomSample();
        Sample sampleWithDomain = this.faker.getBuilderFromSample(sample).withDomain(this.faker.generateTestDomain()).build();
        when(aapService.handleSampleDomain(sample)).thenReturn(sampleWithDomain);

        this.mockMvc.perform(
                post("/biosamples/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serialize(sample))
                        .header("Authorization", "Bearer $TOKEN"))
                .andExpect(status().is2xxSuccessful())
                .andDo(document("sample-submission", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
    }


    @Test
    @Ignore
    public void updateSample() throws Exception {
        Sample sampleWithDomain = this.faker.getBuilderFromSample(this.faker.generateRandomSample())
                .withDomain(this.faker.generateTestDomain())
                .build();

        when(sampleService.fetch(eq(sampleWithDomain.getAccession()))).thenReturn(Optional.of(sampleWithDomain));
        when(sampleService.store(eq(sampleWithDomain))).thenReturn(sampleWithDomain);
        when(aapService.handleSampleDomain(sampleWithDomain)).thenReturn(sampleWithDomain);
        doNothing().when(aapService).checkAccessible(isA(Sample.class));

        this.mockMvc.perform(
                put("/biosamples/samples/"  + sampleWithDomain.getAccession()).contentType(MediaType.APPLICATION_JSON).content(serialize(sampleWithDomain)).header("Authorization", "Bearer $TOKEN"))
                .andExpect(status().is2xxSuccessful())
                .andDo(document("sample-update", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));

    }

    @Test
    public void curateSample() throws Exception {
        CurationLink curationLink = this.faker.getExampleCurationLinkObject();
        when(aapService.handleCurationLinkDomain(eq(curationLink))).thenReturn(curationLink);
        when(curationPersistService.store(curationLink)).thenReturn(curationLink);

        this.mockMvc.perform(
                post("/biosamples/samples/{accession}/curationlinks", curationLink.getSample()).contentType(MediaType.APPLICATION_JSON).content(serialize(curationLink)).header("Authorization", "Bearer $TOKER"))
                .andExpect(status().is2xxSuccessful())
                .andDo(document("curation-submission", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
    }

    // TODO Move all the subsequent to DocumentationHelper class
    private Pageable getDefaultPageable() {
        return new PageRequest(0, 20, getDefaultSort());
    }

    private Sort getDefaultSort() {
        return new Sort(Stream.of("score,desc", "id,asc").map(this::parseSort).collect(Collectors.toList()));
    }

    private Sort.Order parseSort(String sort) {
        if(sort.endsWith(",desc")) {
            return new Sort.Order(Sort.Direction.DESC, sort.substring(0, sort.length()-5));
        } else if(sort.endsWith(",asc")) {
            return new Sort.Order(Sort.Direction.ASC, sort.substring(0, sort.length()-4));
        } else {
            return new Sort.Order(null, sort);
        }
    }

    private String serialize(Object obj) throws JsonProcessingException {
        return mapper.writeValueAsString(obj);
    }
}
