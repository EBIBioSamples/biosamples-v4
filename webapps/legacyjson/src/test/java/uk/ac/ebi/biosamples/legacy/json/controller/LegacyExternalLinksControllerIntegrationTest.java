package uk.ac.ebi.biosamples.legacy.json.controller;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.hateoas.MediaTypes.HAL_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
public class LegacyExternalLinksControllerIntegrationTest {

	@MockBean
	private SampleRepository sampleRepositoryMock;

	@Autowired
    private MockMvc mockMvc;

	@Test
	public void testExternalLinksIndexReturnPagedResourcesOfExternalLinks() throws Exception {
	    mockMvc.perform(get("/externallinksrelations").accept(HAL_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/hal+json;charset=UTF-8"))
				.andExpect(jsonPath("$").value(
						allOf(hasKey("_embedded"), hasKey("_links"), hasKey("page")
				)));
	}

	@Test
	@Ignore
	public void testReturnExternalLinkByLinkName() throws Exception {
	    /*TODO */
	}

	@Test
	@Ignore
	public void textExternalLinksContainsExpectedLinks() throws Exception {
	/* TODO */
	}

	@Test
	public void testExternalLinksIndexContainsSearchLink() throws Exception {
	    mockMvc.perform(get("/externallinksrelations").accept(HAL_JSON))
				.andExpect(jsonPath("$._links").value(hasKey("search")));

	}

	@Test
	@Ignore
	public void testExternalLinksSearchContainsFindOneByUrlLink() throws Exception {
	    /*TODO */
	}

	@Test
	@Ignore
	public void testFindOneByUrlSearchReturnExternalLink() throws Exception {
	    /*TODO */
	}
	
	
	@Test
	@Ignore
	public void testOrganizationIsRootField() throws Exception {
	    /*TODO */
	}
	
}
