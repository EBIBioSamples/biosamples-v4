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
import uk.ac.ebi.biosamples.legacy.json.domain.TestSample;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.Optional;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.hateoas.MediaTypes.HAL_JSON;


@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
public class LegacyGroupsControllerIntegrationTest {

	@MockBean
	private SampleRepository sampleRepository;

	@Autowired
    private MockMvc mockMvc;

	@Test
	public void testRetrieveOfGroupByAccession() throws Exception {
	    Sample group = new TestSample("SAMEG1").build();
	    when(sampleRepository.findByAccession("SAMEG1")).thenReturn(Optional.of(group));

	    mockMvc.perform(get("/groups/SAMEG1").accept(HAL_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/hal+json;charset=UTF-8"))
				.andExpect(jsonPath("$.accession").value("SAMEG1"));
	}

	@Test //FIXME BioSamples v3 had Name,Url and Accession as property of external reference
	public void testExternalLinksIsRootField() throws Exception {
	    Sample group = new TestSample("SAMEG1")
				.withExternalReference(ExternalReference.build("http://test.com/1"))
				.withExternalReference(ExternalReference.build("http://test.com/2"))
				.build();

	    when(sampleRepository.findByAccession(group.getAccession())).thenReturn(Optional.of(group));

	    mockMvc.perform(get("/groups/{accession}", group.getAccession()).accept(HAL_JSON))
				.andExpect(jsonPath("$.externalReferences")
						.value("[{\"URL\":\"http://test.com/1\"},{\"URL\":\"http://test.com/2\"}]"));


	}

	@Test
	public void testGroupContainsListOfAssociatedSamples() throws Exception {
		Sample group = new TestSample("SAMEG1")
				.withRelationship(Relationship.build("SAMEG1", "has member", "SAMEA1"))
				.withRelationship(Relationship.build("SAMEG1", "has member", "SAMEA2"))
				.build();
		when(sampleRepository.findByAccession(group.getAccession())).thenReturn(Optional.of(group));
		mockMvc.perform(get("/groups/{accession}",group.getAccession()).accept(HAL_JSON))
				.andExpect(jsonPath("$.samples").value( containsInAnyOrder("SAMEA1","SAMEA2") ));
	}

	@Test
	public void testGroupHasProperLinks() throws Exception {
	    Sample group = new TestSample("SAMEG1").build();
	    when(sampleRepository.findByAccession(group.getAccession())).thenReturn(Optional.of(group));

	    mockMvc.perform(get("/groups/{accession}", group.getAccession()).accept(HAL_JSON))
				.andExpect(jsonPath("$._links").value(
						allOf(
								hasKey("self"),
								hasKey("group"),
								hasKey("relations")
						)
				));
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
