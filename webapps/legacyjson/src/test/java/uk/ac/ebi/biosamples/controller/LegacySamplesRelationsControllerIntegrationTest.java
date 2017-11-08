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
import org.springframework.test.web.servlet.ResultActions;
import uk.ac.ebi.biosamples.TestSample;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
public class LegacySamplesRelationsControllerIntegrationTest {

    @MockBean
    private SampleService sampleService;

    @Autowired
    private MockMvc mockMvc;

    private ResultActions getRelationsHAL(String accession) throws Exception {
        return mockMvc.perform(get("/samplesrelations/{accession}", accession).accept(MediaTypes.HAL_JSON_VALUE));
    }

    @Test
    public void testReturnRelationByAccession() throws Exception {
        Sample testSample = new TestSample("RELATION").build();
        when(sampleService.findByAccession(anyString())).thenReturn(testSample);

        getRelationsHAL("anyAccession")
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/hal+json;charset=UTF-8"))
                .andExpect(jsonPath("$.accession").value(testSample.getAccession()));
    }

    @Test
    public void testRelationsHasSelfLink() throws Exception {
        Sample testSample = new TestSample("RELATION").build();
        when(sampleService.findByAccession(anyString())).thenReturn(testSample);

        getRelationsHAL("anyAccession")
                .andExpect(jsonPath("$._links.self").exists());
    }

    @Test
    public void testSampleRelationsLinkExistAndMatchSelfLink() throws Exception {
        Sample testSample = new TestSample("RELATION").build();
        when(sampleService.findByAccession(anyString())).thenReturn(testSample);

        getRelationsHAL("anAccession")
                .andExpect(jsonPath("$._links.samplerelations").exists())
                .andExpect(jsonPath("$._links.samplerelations.href").value(Matchers.endsWith("RELATION")))
                .andDo(result -> {

                    String responseBody = result.getResponse().getContentAsString();
                    String sampleRelationsHrefPath ="$._links.samplerelations.href";
                    String selfHrefPath ="$._links.self.href";


                    assertThat(JsonPath.parse(responseBody).read(selfHrefPath).toString())
                            .isEqualTo(JsonPath.parse(responseBody).read(sampleRelationsHrefPath).toString());
                });

    }

    @Test
    public void testSampleRelationsContainsAllExpectedLinks() throws Exception {
        Sample testSample = new TestSample("SAMED1111").build();
        when(sampleService.findByAccession(anyString())).thenReturn(testSample);

        getRelationsHAL(testSample.getAccession())
                .andExpect(jsonPath("$._links").value(
                        allOf(
                                hasKey("self"),
                                hasKey("details"),
                                hasKey("samplerelations"),
                                hasKey("groups"),
                                hasKey("derivedFrom"),
                                hasKey("recuratedFrom"),
                                hasKey("childOf"),
                                hasKey("sameAs"),
                                hasKey("parentOf"),
                                hasKey("derivedTo"),
                                hasKey("recuratedTo"),
                                hasKey("externallinks")
                        )
                ));
    }

    @Test
    public void testSamplesRelationsWithGroupIsReturned() throws Exception {
        Sample testGroup = new TestSample("SAMEG222").build();
        Sample testSample = new TestSample("SAMEA111")
                .withRelationship(Relationship.build("SAMEG222", "has member", "SAMEA111"))
                .build();

        when(sampleService.findByAccession(testSample.getAccession())).thenReturn(testSample);
        when(sampleService.findByAccession(testGroup.getAccession())).thenReturn(testGroup);

        MvcResult result = getRelationsHAL("SAMEA111")
                .andExpect(jsonPath("$._links.groups.href").value(
                        Matchers.endsWith("SAMEA111/groups")
                ))
                .andReturn();

        String groupRelationsHref = JsonPath.parse(result.getResponse().getContentAsString()).read("$._links.groups.href");

        mockMvc.perform(get(groupRelationsHref).accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/hal+json;charset=UTF-8"))
                .andExpect(jsonPath("$._embedded.groupsrelations[0].accession").value("SAMEG222"))
                .andExpect(jsonPath("$._links.self").exists())
                .andExpect(jsonPath("$._links.self.href").value(
                        endsWith("samplesrelations/SAMEA111/groups")
                ));
    }

    @Test
    public void testAnotherSamplesRelationsWithGroupIsReturned() throws Exception {
        Sample testGroup = new TestSample("SAMEG111").build();
        Sample testSample = new TestSample("SAMEA222")
                .withRelationship(Relationship.build("SAMEG111", "has member", "SAMEA222"))
                .build();

        when(sampleService.findByAccession(testSample.getAccession())).thenReturn(testSample);
        when(sampleService.findByAccession(testGroup.getAccession())).thenReturn(testGroup);

        MvcResult result = getRelationsHAL("SAMEA222")
                .andExpect(jsonPath("$._links.groups.href").value(
                        endsWith("SAMEA222/groups")
                ))
                .andReturn();

        String groupRelationsHref = JsonPath.parse(result.getResponse().getContentAsString()).read("$._links.groups.href");

        mockMvc.perform(get(groupRelationsHref).accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/hal+json;charset=UTF-8"))
                .andExpect(jsonPath("$._embedded.groupsrelations[0].accession").value("SAMEG111"))
                .andExpect(jsonPath("$._links.self").exists())
                .andExpect(jsonPath("$._links.self.href").value(
                        endsWith("samplesrelations/SAMEA222/groups")
                ));
    }

    @Test
    public void testDeriveToRelationIsReturned() throws Exception {
        Sample sample1 = new TestSample("SAMPLE1")
                .withRelationship(Relationship.build(
                        "SAMPLE2", "derivedFrom", "SAMPLE1"
                )).build();
        Sample sample2 = new TestSample("SAMPLE2")
                .withRelationship(Relationship.build(
                        "SAMPLE2", "derivedFrom", "SAMPLE1"
                )).build();

        when(sampleService.findByAccession(sample1.getAccession())).thenReturn(sample1);
        when(sampleService.findByAccession(sample2.getAccession())).thenReturn(sample2);

        String responseContent = getRelationsHAL(sample2.getAccession())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.derivedFrom.href").value(
                        endsWith("SAMPLE2/derivedFrom")
                ))
                .andReturn().getResponse().getContentAsString();

        String derivedFromHref = JsonPath.parse(responseContent).read("$._links.derivedFrom.href");

        mockMvc.perform(get(derivedFromHref).accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.samplesrelations[0].accession").value(sample1.getAccession()));

    }

    @Test
    public void testDerivedFromAndDerivedToAreWorkingAsInverseRelations() throws Exception{
        Sample sample1 = new TestSample("SAMPLE1")
                .withRelationship(Relationship.build(
                        "SAMPLE1", "derivedTo", "SAMPLE2"
                )).build();
        Sample sample2 = new TestSample("SAMPLE2")
                .withRelationship(Relationship.build(
                        "SAMPLE1", "derivedTo", "SAMPLE2"
                )).build();

        when(sampleService.findByAccession(sample1.getAccession())).thenReturn(sample1);
        when(sampleService.findByAccession(sample2.getAccession())).thenReturn(sample2);


        String samplesRelationsResponseContent = getRelationsHAL(sample2.getAccession())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.derivedFrom.href").value(endsWith("SAMPLE2/derivedFrom")))
                .andReturn().getResponse().getContentAsString();

        String derivedFromHref = JsonPath.parse(samplesRelationsResponseContent).read("$._links.derivedFrom.href");

        String derivedToSamplesResponseContent = mockMvc.perform(get(derivedFromHref).accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.samplesrelations[0].accession").value(sample1.getAccession()))
                .andReturn().getResponse().getContentAsString();

        String derivedToHref = JsonPath
                .parse(derivedToSamplesResponseContent)
                .read("$._embedded.samplesrelations[0]._links.derivedTo.href");

        mockMvc.perform(get(derivedToHref).accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.samplesrelations[0].accession").value(sample2.getAccession()));
    }

}
