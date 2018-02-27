package uk.ac.ebi.biosamples.docs;

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
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.SamplePageService;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
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
    private BioSamplesAapService aapService;

    private DocumentationHelper faker;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        this.faker = new DocumentationHelper();
//        Faker faker = new Faker();
//        String name = faker.job()
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)

                .apply(documentationConfiguration(this.restDocumentation).uris()
                    .withScheme("https")
                    .withHost("www.ebi.ac.uk")
                    .withPort(443)
                )
                .defaultRequest(get("/").contextPath("/biosamples"))
                .build();
    }

    @Test
    @Ignore
    public void apiRoot() throws Exception {
        this.mockMvc.perform(get("/biosamples").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(document("index", links(halLinks(),
                        linkWithRel("samples").description("Link to all the sample resources"),
                        linkWithRel("curations").description("Link to all the curation resources")
                )));

    }

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
    public void submitSample() throws Exception {
        Sample sample = this.faker.generateRandomSample();
        String sampleSerialized = mapper.writeValueAsString(sample);

        Sample sampleWithDomain = this.faker.getBuilderFromSample(sample).withDomain(this.faker.generateRandomDomain()).build();
        when(aapService.handleSampleDomain(sample)).thenReturn(sampleWithDomain);

        this.mockMvc.perform(
                post("/biosamples/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleSerialized)
                        .header("Authorization", "Bearer $TOKEN"))
                .andExpect(status().is2xxSuccessful())
                .andDo(document("sample-submission", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));


    }


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

}
