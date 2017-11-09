package uk.ac.ebi.biosamples.controller;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.ac.ebi.biosamples.TestAttribute;
import uk.ac.ebi.biosamples.TestSample;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleRepository;

import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
public class LegacySamplesControllerIntegrationTest {

	@MockBean
	private SampleRepository sampleRepositoryMock;

	@Autowired
    private MockMvc mockMvc;

	@Test
	public void testReturnSampleByAccession() throws Exception {
		Sample testSample = new TestSample("SAMEA123123").build();
		when(sampleRepositoryMock.findByAccession(testSample.getAccession())).thenReturn(testSample);

    	mockMvc.perform(
    			get("/samples/{accession}", testSample.getAccession())
					.accept("application/hal+json;charset=UTF-8"))
                .andExpect(status().isOk())
				.andExpect(jsonPath("$.accession").value("SAMEA123123"));
	}

	@Test
	public void testResponseContentTypeIsHalJson() throws Exception {
		Sample testSample = new TestSample("SAMEA0").build();
		when(sampleRepositoryMock.findByAccession(testSample.getAccession())).thenReturn(testSample);

		mockMvc.perform(get("/samples/SAMEA0").accept("application/hal+json;charset=UTF-8"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/hal+json;charset=UTF-8"));
	}

	@Test
	public void testSampleDescriptionIsExposedAsRootAttribute() throws Exception {
		Sample testSample = new TestSample("SAMEA1")
				.withAttribute(new TestAttribute("description", "simple description").build())
				.build();
		when(sampleRepositoryMock.findByAccession(testSample.getAccession())).thenReturn(testSample);

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
		when(sampleRepositoryMock.findByAccession(testSample.getAccession())).thenReturn(testSample);

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

		when(sampleRepositoryMock.findByAccession(testSample.getAccession())).thenReturn(testSample);

		mockMvc.perform(get("/samples/SAMEA0").accept(MediaTypes.HAL_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.characteristics.type[0].ontologyTerms").isArray())
				.andExpect(jsonPath("$.characteristics.type[0].ontologyTerms[0]").value("test"));
	}

	@Test
	public void testSampleNameIsExposedAsRootField() throws Exception {
		Sample testSample = new TestSample("SAMEA2").withName("StrangeName").build();
		when(sampleRepositoryMock.findByAccession(testSample.getAccession())).thenReturn(testSample);

		mockMvc.perform(get("/samples/SAMEA2").accept(MediaTypes.HAL_JSON_VALUE))
				.andExpect(jsonPath("$.name").value("StrangeName"));

	}

	@Test
	public void testSampleResourceHasLinksForSampleSelfAndRelationships() throws Exception {
		Sample testSample = new TestSample("SAMN12").build();
		when(sampleRepositoryMock.findByAccession(testSample.getAccession())).thenReturn(testSample);

		mockMvc.perform(
				get("/samples/{accession}", testSample.getAccession())
						.accept(MediaTypes.HAL_JSON))
				.andExpect(jsonPath("$._links").value(
				        Matchers.allOf(
				        		Matchers.hasKey("sample"),
								Matchers.hasKey("self"),
								Matchers.hasKey("relations")
						)
				));
	}

	@Test
	public void testSampleResourceLinkForSampleEqualsLinkForSelf() throws Exception {
		Sample testSample = new TestSample("SAMEA555").build();
		when(sampleRepositoryMock.findByAccession(testSample.getAccession())).thenReturn(testSample);

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
		when(sampleRepositoryMock.findByAccession(testSample.getAccession())).thenReturn(testSample);

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


}
