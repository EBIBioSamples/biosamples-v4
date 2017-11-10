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
public class LegacyGroupsControllerIntegrationTest {

	@MockBean
	private SampleRepository sampleRepositoryMock;

	@Autowired
    private MockMvc mockMvc;

	@Test
	@Ignore
	public void testRetrieveOfGroupByAccession() throws Exception {
	    /*TODO */
	}

	@Test
	@Ignore
	public void testExternalLinksIsRootField() throws Exception {
	    /*TODO */
	}

	@Test
	@Ignore
	public void testGroupContainsListOfAssociatedSamples() throws Exception {
	    /*TODO */
	}

	@Test
	@Ignore
	public void testGroupHasProperLinks() throws Exception {
	    /*TODO */
	}

	@Test
	@Ignore
	public void testGroupsIndexReturnPagedResources() throws Exception {
	    /*TODO */
	}

	@Test
	@Ignore
	public void testGroupsIndexHasSearchLink() throws Exception {
	    /*TODO */
	}

	@Test
	@Ignore
	public void testGroupsSearchHasExpectedLinks() throws Exception {
	    /*TODO */
	}

	@Test
	@Ignore
	public void testSearchGroupsByKeyword() throws Exception {
	    /*TODO */
	}

	@Test
	@Ignore
	public void testSearchGroupsByAccession() throws Exception {
	    /*TODO */
	}
	

	@Test
	@Ignore
	public void testContactIsRootField() throws Exception {
		/*TODO*/
	}

	@Test
	@Ignore
	public void testPublicationIsRootField() throws Exception {
	    /*TODO */
	}

	@Test
	@Ignore
	public void testOrganizationIsRootField() throws Exception {
	    /*TODO */
	}
	
}
