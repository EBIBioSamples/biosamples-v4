package uk.ac.ebi.biosamples.legacy.json.controller;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.biosamples.legacy.json.domain.TestSample;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.hateoas.MediaTypes.HAL_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
public class LegacyGroupsControllerIntegrationTest {

	@MockBean
	private SampleRepository sampleRepository;

	@Autowired
    private MockMvc mockMvc;

	@Autowired
	PagedResourcesAssembler<Sample> pagedResourcesAssembler;

	private PagedResources<Resource<Sample>> getTestPagedResourcesGroups(int totalSamples, Sample... samples) {
		List<Sample> allSamples = Stream.of(samples)
				.collect(Collectors.toList());

		Pageable pageInfo = new PageRequest(0,samples.length);
		Page<Sample> samplePage = new PageImpl<>(allSamples, pageInfo, totalSamples);
		return pagedResourcesAssembler.toResource(samplePage);

	}

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
	public void testGroupsIndexReturnPagedResources() throws Exception {
	    Sample group1 = new TestSample("SAMEG1").build();
	    Sample group2 = new TestSample("SAMEG2").build();
	    when(sampleRepository.findGroups(anyInt(), anyInt())).thenReturn(
	    		getTestPagedResourcesGroups(10, group1, group2));

	    mockMvc.perform(get("/groups").accept(HAL_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/hal+json;charset=UTF-8"))
				.andExpect(jsonPath("$").value(
						allOf(
								hasKey("_embedded"),
								hasKey("_links"),
								hasKey("page")
						)
				))
				.andExpect(jsonPath("$._embedded.groups.*.accession").value(
						containsInAnyOrder("SAMEG1", "SAMEG2")
				));


	}

	@Test
	public void testGroupsIndexHasSearchLink() throws Exception {
	    mockMvc.perform(get("/groups").accept(HAL_JSON))
				.andExpect(jsonPath("$._links").value(hasKey("search")));
	}

	@Test
	public void testGroupsSearchHasExpectedLinks() throws Exception {
		mockMvc.perform(get("/groups/search").accept(HAL_JSON))
				.andExpect(jsonPath("$._links").value(
								allOf(hasKey("self"), hasKey("findByKeywords"), hasKey("findByAccession"))));
	}

	@Test
	public void testSearchGroupsByKeyword() throws Exception {
	   /*TODO test in end-to-end tests*/
	}

	@Test
	@Ignore
	public void testSearchGroupsByAccession() throws Exception {
	    /*TODO test in end-to-end tests*/
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
