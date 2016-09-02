package uk.ac.ebi.biosamples;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@TestPropertySource(locations="classpath:test.properties")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MockMVCDocumentation {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Rule
	public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("target/generated-snippets");
	
	@Autowired
	private WebApplicationContext context;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(documentationConfiguration(this.restDocumentation)).build();
	}
	
	@Test
	public void indexExample() throws Exception {
		log.info("Trying to create index-example");
		this.mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andDo(document("index-example",
					links(
							linkWithRel("samplesrelations").description("Relationships between samples"),
							linkWithRel("profile").description("The ALPS profile for the service")),
					responseFields(
							fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources"))));
	}
}
