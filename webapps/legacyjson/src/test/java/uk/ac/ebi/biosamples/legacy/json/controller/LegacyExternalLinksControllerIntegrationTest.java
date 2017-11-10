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


@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
public class LegacyExternalLinksControllerIntegrationTest {

	@MockBean
	private SampleRepository sampleRepositoryMock;

	@Autowired
    private MockMvc mockMvc;

	@Test
	@Ignore
	public void testReturnExternalLinkByLinkName() throws Exception {
	    /*TODO */
	}

	@Test
	@Ignore
	public void textExternalLinksContainsExpectedLinks() throws Exception {
	    /*TODO */
	}

	@Test
	@Ignore
	public void testExternalLinksIndexReturnPagedResourcesOfExternalLinks() throws Exception {
	    /*TODO */
	}

	@Test
	@Ignore
	public void testExternalLinksIndexContainsSearchLink() throws Exception {
	    /*TODO */
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
