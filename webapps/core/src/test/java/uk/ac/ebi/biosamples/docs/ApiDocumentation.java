package uk.ac.ebi.biosamples.docs;

import com.github.javafaker.Faker;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.biosamples.controller.SampleRestController;
import uk.ac.ebi.biosamples.controller.SamplesRestController;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.SamplePageService;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.halLinks;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiDocumentation {

    @Rule
    public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("target/generated-snippets");

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private SamplePageService samplePageService;

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
                .build();
    }

    @Test
    public void apiRoot() throws Exception {
        this.mockMvc.perform(get("/biosamples").accept(MediaTypes.HAL_JSON).contextPath("/biosamples"))
                .andExpect(status().isOk())
                .andDo(document("index", links(halLinks(),
                        linkWithRel("samples").description("Link to all the sample resources"),
                        linkWithRel("curations").description("Link to all the curation resources")
                )));

    }

    @Test
    public void samplesRoot() throws Exception {
        Page<Sample> samplePage = new PageImpl<>(this.faker.generateRandomSamples(100), getDefaultPageable(), 100);
//        when(samplesRestControllerMock.searchHal(null, null, null, null, null, null, null)).thenReturn(resources);
        when(samplePageService.getSamplesByText(any(String.class), anyCollectionOf(Filter.class), anyCollectionOf(String.class), eq(getDefaultPageable())))
                .thenReturn(samplePage);
        this.mockMvc.perform(get("/biosamples/samples").accept(MediaTypes.HAL_JSON).contextPath("/biosamples"))
                .andExpect(status().isOk())
                .andDo(document("samples"));
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
