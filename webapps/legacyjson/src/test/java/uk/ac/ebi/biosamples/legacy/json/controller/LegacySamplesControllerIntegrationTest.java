package uk.ac.ebi.biosamples.legacy.json.controller;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
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
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.UriTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.ac.ebi.biosamples.legacy.json.domain.TestAttribute;
import uk.ac.ebi.biosamples.legacy.json.domain.TestSample;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
public class LegacySamplesControllerIntegrationTest {

	@MockBean
	private SampleRepository sampleRepository;

	@Autowired
    private MockMvc mockMvc;

	@Autowired
	PagedResourcesAssembler<Sample> pagedResourcesAssembler;

	private PagedResources<Resource<Sample>> getTestPagedResourcesSample(int totalSamples, Sample... samples) {
		List<Sample> allSamples = Stream.of(samples)
				.collect(Collectors.toList());

		Pageable pageInfo = new PageRequest(0,samples.length);
		Page<Sample> samplePage = new PageImpl<>(allSamples, pageInfo, totalSamples);
		return pagedResourcesAssembler.toResource(samplePage);

	}

	@Test
	public void testReturnSampleByAccession() throws Exception {
		Sample testSample = new TestSample("SAMEA123123").build();
		when(sampleRepository.findByAccession(testSample.getAccession())).thenReturn(Optional.of(testSample));

    	mockMvc.perform(
    			get("/samples/{accession}", testSample.getAccession())
					.accept("application/hal+json;charset=UTF-8"))
                .andExpect(status().isOk())
				.andExpect(jsonPath("$.accession").value("SAMEA123123"));
	}

	@Test
	public void testResponseContentTypeIsHalJson() throws Exception {
		Sample testSample = new TestSample("SAMEA0").build();
		when(sampleRepository.findByAccession(testSample.getAccession())).thenReturn(Optional.of(testSample));

		mockMvc.perform(get("/samples/SAMEA0").accept("application/hal+json;charset=UTF-8"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/hal+json;charset=UTF-8"));
	}

	@Test
	public void testSampleDescriptionIsExposedAsRootAttribute() throws Exception {
		Sample testSample = new TestSample("SAMEA1")
				.withAttribute(new TestAttribute("description", "simple description").build())
				.build();
		when(sampleRepository.findByAccession(testSample.getAccession())).thenReturn(Optional.of(testSample));

		mockMvc.perform(
				get("/samples/{accession}", "SAMEA1").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
				.andExpect(jsonPath("$.description").value("simple description"));

	}

	@Test
	public void testSampleReleaseDateFormatAsLocalDate() throws Exception {

		Sample testSample = new TestSample("SAMEA1")
				.releasedOn(Instant.parse("2016-01-01T00:30:00Z"))
				.build();
		when(sampleRepository.findByAccession(testSample.getAccession())).thenReturn(Optional.of(testSample));

		mockMvc.perform(get("/samples/SAMEA1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.releaseDate").value("2016-01-01"));

	}

	@Test
	public void testSampleOntologyURIsIsAlwaysAnArray() throws Exception {

		Sample testSample = new TestSample("SAMEA0")
				.withAttribute(
						new TestAttribute("type", "value").withOntologyUri("test").build())
				.build();

		when(sampleRepository.findByAccession(testSample.getAccession())).thenReturn(Optional.of(testSample));

		mockMvc.perform(get("/samples/SAMEA0").accept(MediaTypes.HAL_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.characteristics.type[0].ontologyTerms").isArray())
				.andExpect(jsonPath("$.characteristics.type[0].ontologyTerms[0]").value("test"));
	}

	@Test
	public void testSampleNameIsExposedAsRootField() throws Exception {
		Sample testSample = new TestSample("SAMEA2").withName("StrangeName").build();
		when(sampleRepository.findByAccession(testSample.getAccession())).thenReturn(Optional.of(testSample));

		mockMvc.perform(get("/samples/SAMEA2").accept(MediaTypes.HAL_JSON_VALUE))
				.andExpect(jsonPath("$.name").value("StrangeName"));

	}

	@Test
	public void testSampleResourceHasLinksForSampleSelfAndRelationships() throws Exception {
		Sample testSample = new TestSample("SAMN12").build();
		when(sampleRepository.findByAccession(testSample.getAccession())).thenReturn(Optional.of(testSample));

		mockMvc.perform(
				get("/samples/{accession}", testSample.getAccession())
						.accept(MediaTypes.HAL_JSON))
				.andExpect(jsonPath("$._links").value(
				        allOf(
				        		Matchers.hasKey("sample"),
								Matchers.hasKey("self"),
								Matchers.hasKey("relations")
						)
				));
	}

	@Test
	public void testSampleResourceLinkForSampleEqualsLinkForSelf() throws Exception {
		Sample testSample = new TestSample("SAMEA555").build();
		when(sampleRepository.findByAccession(testSample.getAccession())).thenReturn(Optional.of(testSample));

		mockMvc.perform(
				get("/samples/{accession}", testSample.getAccession())
				.accept(MediaTypes.HAL_JSON))
				.andDo(result-> {
					String responseContent = result.getResponse().getContentAsString();
					String sampleHref = JsonPath.parse(responseContent).read("$._links.sample.href");
					String selfHref = JsonPath.parse(responseContent).read("$._links.self.href");
					assertEquals(sampleHref, selfHref);
				});
	}

	@Test
	public void testUsingRelationsLinkGetsARelationsResource() throws Exception {
		Sample testSample = new TestSample("SAMED666").withRelationship(
				Relationship.build("SAMED666", "deriveFrom", "SAMED555")
		).build();
		when(sampleRepository.findByAccession(testSample.getAccession())).thenReturn(Optional.of(testSample));

		MvcResult result = mockMvc.perform(get("/samples/{accession}", testSample.getAccession())
				.accept(MediaTypes.HAL_JSON))
				.andExpect(jsonPath("$._links.relations.href").value(
						Matchers.endsWith("/samplesrelations/SAMED666")
				))
				.andReturn();

		String deriveFromLink = JsonPath.parse(result.getResponse().getContentAsString()).read("$._links.relations.href");
		mockMvc.perform(get(deriveFromLink).accept(MediaTypes.HAL_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accession").value("SAMED666"));
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
	
	@Test
	public void testRetrieveAllSamplesAsPagedResources() throws Exception {
		Sample sampleA = new TestSample("A").build();
		Sample sampleB = new TestSample("B")
				.withAttribute(new TestAttribute("Organism", "Homo sapiens").build())
				.build();
		when(sampleRepository.getPagedSamples(anyInt(), anyInt())).thenReturn(getTestPagedResourcesSample(10, sampleA, sampleB));

		mockMvc.perform(get("/samples").accept(MediaTypes.HAL_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/hal+json;charset=UTF-8"))
				.andExpect(jsonPath("$").isMap())
				.andExpect(jsonPath("$..accession").value(
						containsInAnyOrder("A","B")
				))
				.andExpect(jsonPath("$._embedded.samples[?(@.accession=='B')].characteristics.Organism.*.text").value("Homo sapiens"));

	}

	@Test
	public void testAllSamplesLinksContainsSearch() throws Exception {

		Sample sampleA = new TestSample("A").build();
		Sample sampleB = new TestSample("B")
				.withAttribute(new TestAttribute("Organism", "Homo sapiens").build())
				.build();
		when(sampleRepository.getPagedSamples(anyInt(), anyInt())).thenReturn(getTestPagedResourcesSample(10, sampleA, sampleB));

		String responseContent = mockMvc.perform(get("/samples").accept(MediaTypes.HAL_JSON))
				.andExpect(jsonPath("$._links.search").exists())
				.andReturn().getResponse().getContentAsString();

		String searchEndpoint = JsonPath.parse(responseContent)
							.read("$._links.search.href");

		mockMvc.perform(get(searchEndpoint).accept(MediaTypes.HAL_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/hal+json;charset=UTF-8"))
				.andExpect(jsonPath("$._embedded").doesNotExist())
				.andExpect(jsonPath("$._links").isNotEmpty())
				.andExpect(jsonPath("$._links").value(
						allOf(
								hasKey("findFirstByGroupsContains"),
								hasKey("findByGroups"),
								hasKey("findByAccession"),
								hasKey("findByText"),
								hasKey("findByTextAndGroups"),
								hasKey("findByAccessionAndGroups"),
								hasKey("self")
						)
				));


	}


	@Test
	public void testFindFirstByGroupFunctionality() throws Exception {
	    Sample sampleA = new TestSample("A").build();
	    when(sampleRepository.findFirstByGroup("groupA")).thenReturn(Optional.of(new Resource(sampleA)));
		when(sampleRepository.findFirstByGroup("groupB")).thenReturn(Optional.empty());

		String searchLinkContent = mockMvc.perform(get("/samples/search").accept(MediaTypes.HAL_JSON))
				.andExpect(jsonPath("$._links.findFirstByGroupsContains.href").value(endsWith("{?group}")))
				.andReturn().getResponse().getContentAsString();

		UriTemplate findFirstByGroupTemplateUrl = new UriTemplate(
				JsonPath.parse(searchLinkContent).read("$._links.findFirstByGroupsContains.href"));


		mockMvc.perform(get(findFirstByGroupTemplateUrl.expand("groupA")).accept(MediaTypes.HAL_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/hal+json;charset=UTF-8"))
				.andExpect(jsonPath("$.accession").value("A"));

		mockMvc.perform(get(findFirstByGroupTemplateUrl.expand("groupB")).accept(MediaTypes.HAL_JSON))
				.andExpect(status().isNotFound());

	}
	
	@Test
	public void testFindByGroupFunctionality() throws Exception {
		Sample sampleA = new TestSample("A").withRelationship(
				Relationship.build("groupA", "has member", "A")).build();
		Sample sampleB = new TestSample("B").withRelationship(
				Relationship.build("groupA", "has member", "B")).build();
		Sample groupA = new TestSample("groupA")
				.withRelationship(Relationship.build("groupA", "has member", "A"))
				.withRelationship(Relationship.build("groupA", "has member", "B"))
				.build();

		when(sampleRepository.findByGroup(eq("groupA"), anyInt(), anyInt()))
				.thenReturn(getTestPagedResourcesSample(2, sampleA, sampleB));

		String searchLinkContent = mockMvc.perform(get("/samples/search").accept(MediaTypes.HAL_JSON))
				.andExpect(jsonPath("$._links.findByGroups.href").value(endsWith("{?group,size,page,sort}")))
				.andReturn().getResponse().getContentAsString();

		UriTemplate findFirstByGroupTemplateUrl = new UriTemplate(
				JsonPath.parse(searchLinkContent).read("$._links.findByGroups.href"));
		Map<String,String> urlParameters = new HashMap<>();
		urlParameters.put("group", "groupA");
		urlParameters.put("page", "0");
		urlParameters.put("size", "50");

		mockMvc.perform(get(findFirstByGroupTemplateUrl.expand(urlParameters)).accept(MediaTypes.HAL_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/hal+json;charset=UTF-8"))
				.andExpect(jsonPath("$._embedded.samples.*.accession").value(containsInAnyOrder("A", "B")));

	}
	
	
}
