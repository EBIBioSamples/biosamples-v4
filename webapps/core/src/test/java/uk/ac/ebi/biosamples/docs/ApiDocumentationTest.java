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
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiDocumentationTest {

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
    CurationReadService curationReadService;

    @MockBean
    private BioSamplesAapService aapService;

    private DocumentationHelper faker;
    private MockMvc mockMvc;

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
    public void getIndex() throws Exception {
        this.mockMvc.perform(get("/biosamples").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(document("get-index", links(halLinks(),
                        linkWithRel("samples").description("Link to all the sample resources"),
                        linkWithRel("curations").description("Link to all the curation resources")
                )));

    }

    /**
     * Generate the snippets for the samples root page
     * @throws Exception
     */
    @Test
    public void getSamples() throws Exception {
        Page<Sample> samplePage = new PageImpl<>(Collections.singletonList(this.faker.generateRandomSample()), getDefaultPageable(), 100);
        when(samplePageService.getSamplesByText(any(String.class), anyCollectionOf(Filter.class), anyCollectionOf(String.class), isA(Pageable.class)))
                .thenReturn(samplePage);
        this.mockMvc.perform(get("/biosamples/samples").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(document("get-samples",  requestParameters(
                        parameterWithName("page").description("The page to retrieve").optional(),
                        parameterWithName("size").description("Entries per page").optional(),
                        parameterWithName("text").description("Text to search").optional(),
                        parameterWithName("filter").description("List of filters to apply to search results").optional()
                ), links(halLinks(),
                    linkWithRel("self").description("This resource list"),
                    linkWithRel("first").description("The first page in the resource list"),
                    linkWithRel("next").description("The next page in the resource list, if available").optional()    ,
                    linkWithRel("prev").description("The previous page in the resource list, if available").optional(),
                    linkWithRel("last").description("The last page in the resource list"),
                    linkWithRel("autocomplete").description("List of autocompletion terms for the query"),
                    linkWithRel("facet").description("Link to facet associated with the results"),
                        linkWithRel("sample").description("Link to single sample resource"),
                        linkWithRel("cursor").description("Link to cursor over sample resources")
                )/*,responseFields(
                    fieldWithPath("_links").description("<<resources-page-links,Links>> to other resources"),
                    fieldWithPath("_embedded").description("The list of resources"),
                    fieldWithPath("page.size").description("The number of resources in this page"),
                    fieldWithPath("page.totalElements").description("The total number of resources"),
                    fieldWithPath("page.totalPages").description("The total number of pages"),
                    fieldWithPath("page.number").description("The page number")
                )*/
                ));
    }

    /**
     * Describe what's the minimal information necessary to submit a sample
     * @throws Exception
     */
    @Test
    @Ignore
    public void postSampleMinimalInfo() throws Exception {
        String wrongSampleSerialized = "{\"name\": \"Sample without minimum information\"," +
                " \"accession\": \"SAMEA123123123\"}";

        this.mockMvc.perform(
                post("/biosamples/samples")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(wrongSampleSerialized)
                    .header("Authorization", "Bearer $TOKEN"))
                .andExpect(status().is4xxClientError())
                .andDo(document("post-sample-minimal-information", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
    }

    /**
     * Describes what's the error when curationLink minimal information is not provided
     * @throws Exception
     */
    @Test
    @Ignore
    public void postCurationLinkMinimalInfo() throws Exception {

        String wrongSampleSerialized = "{\"sample\": \"SAMEA123123123\", \"curation\": {}}";

        this.mockMvc.perform(
                post("/biosamples/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongSampleSerialized)
                        .header("Authorization", "Bearer $TOKEN"))
                .andExpect(status().is4xxClientError())
                .andDo(document("post-curation-minimal-information", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
    }

    /**
     * Generate the snippets for Sample submission to BioSamples
     * @throws Exception
     */
    @Test
    public void postSample() throws Exception {
        Sample sample = this.faker.generateRandomSample();
        Sample sampleWithDomain = this.faker.getBuilderFromSample(sample).withDomain(this.faker.generateTestDomain()).build();
        when(aapService.handleSampleDomain(eq(sample))).thenReturn(sampleWithDomain);
        when(sampleService.store(any(Sample.class))).thenReturn(sampleWithDomain);

        this.mockMvc.perform(
                post("/biosamples/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serialize(sample))
                        .header("Authorization", "Bearer $TOKEN"))
                .andExpect(status().is2xxSuccessful())
                .andDo(document("post-sample",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        links(halLinks(),
                            linkWithRel("self").ignored(),
                            linkWithRel("curationLinks").description("Link to all the curations associated to the sample")
                        )));
    }

    @Test
    public void putSample() throws Exception {
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
                .andDo(document("put-sample", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));

    }

    @Test
    public void postCurationLink() throws Exception {
        CurationLink curationLink = this.faker.getExampleCurationLinkObject();
        when(aapService.handleCurationLinkDomain(eq(curationLink))).thenReturn(curationLink);
        when(curationPersistService.store(curationLink)).thenReturn(curationLink);

        this.mockMvc.perform(
                post("/biosamples/samples/{accession}/curationlinks", curationLink.getSample()).contentType(MediaType.APPLICATION_JSON).content(serialize(curationLink)).header("Authorization", "Bearer $TOKER"))
                .andExpect(status().is2xxSuccessful())
                .andDo(document("post-curation", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
    }

    @Test
    public void getSample() throws Exception {
        Sample sample = this.faker.getBuilderFromSample(this.faker.generateRandomSample()).withDomain(this.faker.generateTestDomain()).build();
        when(sampleService.fetch(sample.getAccession())).thenReturn(Optional.of(sample));
        doNothing().when(aapService).checkAccessible(isA(Sample.class));

        this.mockMvc.perform(
                get("/biosamples/samples/{accession}", sample.getAccession()).accept(MediaTypes.HAL_JSON))
                .andExpect(status().is2xxSuccessful())
                .andDo(document("get-sample",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        links(halLinks(),
                            linkWithRel("self").ignored(),
                            linkWithRel("curationLinks").description("Link to all the curation resources linked to the sample")
                        )));
    }

    @Test
    public void getCurations() throws Exception {
        Page<Curation> curationPage = new PageImpl<>(Collections.singletonList(this.faker.getExampleCurationLinkObject().getCuration()), getDefaultPageable(), 100);
        when(curationReadService.getPage(isA(Pageable.class))).thenReturn(curationPage);

        this.mockMvc.perform(get("/biosamples/curations").accept(MediaTypes.HAL_JSON))
                .andExpect(status().is2xxSuccessful())
                .andDo(document("get-curations"));

    }

    @Test
    public void getSampleCurationLinks() throws Exception {
        Page<CurationLink> curationLinkPage = new PageImpl<>(Collections.singletonList(this.faker.getExampleCurationLinkObject()), getDefaultPageable(), 100);
        String sampleAccession = curationLinkPage.getContent().get(0).getSample();

        when(curationReadService.getCurationLinksForSample(eq(sampleAccession), isA(Pageable.class))).thenReturn(curationLinkPage);


        this.mockMvc.perform(get("/biosamples/samples/{accession}/curationlinks", sampleAccession).accept(MediaTypes.HAL_JSON))
                .andExpect(status().is2xxSuccessful())
                .andDo(document("get-sample-curation", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
    }

    // TODO Move all the subsequent to DocumentationHelper class
    private Pageable getDefaultPageable() {
        return new PageRequest(0, 1, getDefaultSort());
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
