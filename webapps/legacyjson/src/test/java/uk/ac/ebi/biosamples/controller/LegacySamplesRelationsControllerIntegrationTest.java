package uk.ac.ebi.biosamples.controller;

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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import uk.ac.ebi.biosamples.TestSample;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
public class LegacySamplesRelationsControllerIntegrationTest {

    @MockBean
    private SampleRepository sampleRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    PagedResourcesAssembler<Sample> pagedResourcesAssembler;

    private ResultActions getSamplesRelationsHAL(String accession) throws Exception {
        return mockMvc.perform(get("/samplesrelations/{accession}", accession).accept(MediaTypes.HAL_JSON_VALUE));
    }

    private PagedResources<Resource<Sample>> getTestPagedResourcesSample(int totalSamples, Sample... samples) {
        List<Sample> allSamples = Stream.of(samples)
                .collect(Collectors.toList());

        Pageable pageInfo = new PageRequest(0,samples.length);
        Page<Sample> samplePage = new PageImpl<>(allSamples, pageInfo, totalSamples);
        return pagedResourcesAssembler.toResource(samplePage);

    }

    @Test
    public void testReturnSamplesRelationByAccession() throws Exception {
        Sample testSample = new TestSample("RELATION").build();
        when(sampleRepository.findByAccession(anyString())).thenReturn(Optional.of(testSample));

        getSamplesRelationsHAL("anyAccession")
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/hal+json;charset=UTF-8"))
                .andExpect(jsonPath("$.accession").value(testSample.getAccession()));
    }

    @Test
    public void testSamplesRelationsHasSelfLink() throws Exception {
        Sample testSample = new TestSample("RELATION").build();
        when(sampleRepository.findByAccession(anyString())).thenReturn(Optional.of(testSample));

        getSamplesRelationsHAL("anyAccession")
                .andExpect(jsonPath("$._links.self").exists());
    }

    @Test
    public void testSamplesRelationsLinkExistAndMatchSelfLink() throws Exception {
        Sample testSample = new TestSample("RELATION").build();
        when(sampleRepository.findByAccession(anyString())).thenReturn(Optional.of(testSample));

        getSamplesRelationsHAL("anAccession")
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
    public void testSamplesRelationsContainsAllExpectedLinks() throws Exception {
        Sample testSample = new TestSample("SAMED1111").build();
        when(sampleRepository.findByAccession(anyString())).thenReturn(Optional.of(testSample));

        getSamplesRelationsHAL(testSample.getAccession())
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

        when(sampleRepository.findByAccession(testSample.getAccession())).thenReturn(Optional.of(testSample));
        when(sampleRepository.findByAccession(testGroup.getAccession())).thenReturn(Optional.of(testGroup));

        MvcResult result = getSamplesRelationsHAL("SAMEA111")
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

        when(sampleRepository.findByAccession(testSample.getAccession())).thenReturn(Optional.of(testSample));
        when(sampleRepository.findByAccession(testGroup.getAccession())).thenReturn(Optional.of(testGroup));

        MvcResult result = getSamplesRelationsHAL("SAMEA222")
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
    public void testDeriveToSamplesRelationIsReturned() throws Exception {
        Sample sample1 = new TestSample("SAMPLE1")
                .withRelationship(Relationship.build(
                        "SAMPLE2", "derivedFrom", "SAMPLE1"
                )).build();
        Sample sample2 = new TestSample("SAMPLE2")
                .withRelationship(Relationship.build(
                        "SAMPLE2", "derivedFrom", "SAMPLE1"
                )).build();

        when(sampleRepository.findByAccession(sample1.getAccession())).thenReturn(Optional.of(sample1));
        when(sampleRepository.findByAccession(sample2.getAccession())).thenReturn(Optional.of(sample2));

        String responseContent = getSamplesRelationsHAL(sample2.getAccession())
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
    public void testDerivedFromAndDerivedToWorkAsInverseSamplesRelations() throws Exception{
        Sample sample1 = new TestSample("SAMPLE1")
                .withRelationship(Relationship.build(
                        "SAMPLE1", "derivedTo", "SAMPLE2"
                )).build();
        Sample sample2 = new TestSample("SAMPLE2")
                .withRelationship(Relationship.build(
                        "SAMPLE1", "derivedTo", "SAMPLE2"
                )).build();

        when(sampleRepository.findByAccession(sample1.getAccession())).thenReturn(Optional.of(sample1));
        when(sampleRepository.findByAccession(sample2.getAccession())).thenReturn(Optional.of(sample2));


        String samplesRelationsResponseContent = getSamplesRelationsHAL(sample2.getAccession())
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

    @Test
    public void testSamplesWithNoAssociatedSamplesRelationReturnEmptyResourcesObject() throws Exception {
        Sample testSample = new TestSample("SAMEA1").build();
        when(sampleRepository.findByAccession(testSample.getAccession())).thenReturn(Optional.of(testSample));

        String samplesRelationsContent = getSamplesRelationsHAL(testSample.getAccession()).andReturn().getResponse().getContentAsString();

        String derivedFromHref = JsonPath.parse(samplesRelationsContent).read("$._links.derivedFrom.href");
        mockMvc.perform(get(derivedFromHref).accept(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._embedded.samplesrelations").exists())
                .andExpect(jsonPath("$._embedded.samplesrelations").isEmpty());

    }

    @Test
    public void testSamplesWithNoAssociatedGroupReturnEmptyGroupsRelations() throws Exception {
        Sample testSample = new TestSample("SAMEA1").build();
        when(sampleRepository.findByAccession(testSample.getAccession())).thenReturn(Optional.of(testSample));

        String samplesRelationsContent = getSamplesRelationsHAL(testSample.getAccession()).andReturn().getResponse().getContentAsString();

        String derivedFromHref = JsonPath.parse(samplesRelationsContent).read("$._links.groups.href");
        mockMvc.perform(get(derivedFromHref).accept(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._embedded.groupsrelations").exists())
                .andExpect(jsonPath("$._embedded.groupsrelations").isEmpty());

    }

    @Test
    public void testRetrieveUnknownSamplesRelationsFromLegacyApiThrowsError() throws Exception {
        Sample testSample = new TestSample("anyaccession").build();
        when(sampleRepository.findByAccession(testSample.getAccession())).thenReturn(Optional.of(testSample));

        mockMvc.perform(get("/samplesrelations/anyaccession/unknownRelation").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isBadRequest());

    }

    @Test
    public void testRetrieveAllSamplesRelationsReturnPagedResource() throws Exception {
        Sample sampleA = new TestSample("A").build();
        Sample sampleB = new TestSample("B").build();
        when(sampleRepository.getPagedSamples(anyInt(), anyInt())).thenReturn(getTestPagedResourcesSample(2, sampleA, sampleB));

        mockMvc.perform(get("/samplesrelations").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/hal+json;charset=UTF-8"))
                .andExpect(jsonPath("$._embedded").isNotEmpty())
                .andExpect(jsonPath("$._links").isNotEmpty())
                .andExpect(jsonPath("$.page").isNotEmpty());


    }

    @Test
    public void testRetrieveAllSamplesRelationsOnePerPageEffectivelyReturnOneRelationsPerPage() throws Exception {
        Sample sampleA = new TestSample("A").build();
//        Sample sampleB = new TestSample("B").build();
        when(sampleRepository.getPagedSamples(0, 1)).thenReturn(getTestPagedResourcesSample(2, sampleA));

        mockMvc.perform(get("/samplesrelations?page=0&size=1").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..accession").value("A"))
                .andExpect(jsonPath("$.page.size").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(2))
                .andExpect(jsonPath("$.page.number").value(0));

    }

    @Test
    @Ignore
    public void testAllSamplesRelationsHasLinkToSearch() throws Exception {
        mockMvc.perform(get("/samplesrelations").accept(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._links.search.href").value(
                        endsWith("/samplesrelations/search")
                ));
    }
    
    @Test
    @Ignore
    public void testSamplesRelationsSearchHasLinkFindOneByAccession() throws Exception {
        /*TODO */
    }

    @Test
    @Ignore
    public void testSamplesRelationsFindOneByAccessionReturnTheCorrectSample() throws Exception {
        /*TODO The search by accession of samplesrelations should return the samplesrelations*/
    }

    @Test
    @Ignore
    public void testSamplesRelationsFindOneByAccessionReturn404ForNonExistingSample() throws Exception {
        /*TODO*/
    }

    @Test
    @Ignore
    public void testAllSamplesRelationsHasLinkToProfile() throws Exception {
        /*TODO */
    }
    

    @Test
    @Ignore
    public void shouldReturnOnlySamples() {
        //TODO implement this feature in the end-to-end test
    }

}
