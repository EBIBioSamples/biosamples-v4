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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.ac.ebi.biosamples.legacy.json.domain.TestSample;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.hateoas.MediaTypes.HAL_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
public class LegacyGroupsRelationControllerIntegrationTest {

    @MockBean
    private SampleRepository sampleRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    PagedResourcesAssembler<Sample> pagedResourcesAssembler;

    private ResultActions getGroupsRelationsHAL(String accession) throws Exception {
        return mockMvc.perform(get("/groupsrelations/{accession}", accession).accept(MediaTypes.HAL_JSON_VALUE));
    }

    private PagedResources<Resource<Sample>> getTestPagedResourcesSample(int totalSamples, Sample... samples) {
        List<Sample> allSamples = Stream.of(samples)
                .collect(Collectors.toList());

        Pageable pageInfo = new PageRequest(0,samples.length);
        Page<Sample> samplePage = new PageImpl<>(allSamples, pageInfo, totalSamples);
        return pagedResourcesAssembler.toResource(samplePage);

    }

    @Test
    public void testReturnGroupsRelationByAccession() throws Exception {
        Sample testSample = new TestSample("RELATION").build();
        when(sampleRepository.findByAccession(anyString())).thenReturn(Optional.of(testSample));

        getGroupsRelationsHAL("anyAccession")
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/hal+json;charset=UTF-8"))
                .andExpect(jsonPath("$.accession").value(testSample.getAccession()));
    }

    @Test
    public void testGroupsRelationsHasSelfLink() throws Exception {
        Sample testSample = new TestSample("RELATION").build();
        when(sampleRepository.findByAccession(anyString())).thenReturn(Optional.of(testSample));

        getGroupsRelationsHAL("anyAccession")
                .andExpect(jsonPath("$._links.self").exists());
    }

    @Test
    public void testGroupsRelationsLinkExistAndMatchSelfLink() throws Exception {
        Sample testSample = new TestSample("RELATION").build();
        when(sampleRepository.findByAccession(anyString())).thenReturn(Optional.of(testSample));

        getGroupsRelationsHAL("anAccession")
                .andExpect(jsonPath("$._links.groupsrelations").exists())
                .andExpect(jsonPath("$._links.groupsrelations.href").value(Matchers.endsWith("RELATION")))
                .andDo(result -> {

                    String responseBody = result.getResponse().getContentAsString();
                    String sampleRelationsHrefPath ="$._links.groupsrelations.href";
                    String selfHrefPath ="$._links.self.href";


                    assertThat(JsonPath.parse(responseBody).read(selfHrefPath).toString())
                            .isEqualTo(JsonPath.parse(responseBody).read(sampleRelationsHrefPath).toString());
                });

    }

    @Test
    public void testGroupsRelationsContainsAllExpectedLinks() throws Exception {
        Sample testSample = new TestSample("SAMED1111").build();
        when(sampleRepository.findByAccession(anyString())).thenReturn(Optional.of(testSample));

        getGroupsRelationsHAL(testSample.getAccession())
                .andExpect(jsonPath("$._links").value(
                        allOf(
                                hasKey("self"),
                                hasKey("details"),
                                hasKey("groupsrelations"),
                                hasKey("externallinks"),
                                hasKey("samples")
                        )
                ));
    }

    @Test
    @Ignore
    public void testGoingFromGroupToSampleBackToGroupReturnOriginalGroup() throws Exception {
        //TODO
    }

    @Test
    public void testRequestForUnsupportedRelationThrowsError() throws Exception {
        Sample group = new TestSample("SAMEG1").build();
        mockMvc.perform(get("/groupsrealtions/SAMEG1/test").accept(HAL_JSON))
                .andExpect(status().isNotFound());

    }

    @Test
    public void testRequestForSampleRelationReturnResourcesOfSampleRelations() throws Exception {
        Sample group = new TestSample("SAMEG1")
                .withRelationship(Relationship.build("SAMEG1", "has member", "SAMEA1"))
                .build();
        Sample sample = new TestSample("SAMEA1")
                .withRelationship(Relationship.build("SAMEG1", "has member", "SAMEA1"))
                .build();
        when(sampleRepository.findByAccession(group.getAccession())).thenReturn(Optional.of(group));
        when(sampleRepository.findByAccession(sample.getAccession())).thenReturn(Optional.of(sample));


        mockMvc.perform(get("/groupsrelations/SAMEG1/samples").accept(HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.samplesrelations[0].accession").value("SAMEA1"));
    }

    @Test
    public void testRequestForExternalLinksReturnsExternalLinkResources() throws Exception {
        Sample group = new TestSample("SAMEG1")
                .withExternalReference(ExternalReference.build("http://test/1"))
                .build();
        when(sampleRepository.findByAccession(group.getAccession())).thenReturn(Optional.of(group));


        mockMvc.perform(get("/groupsrelations/SAMEG1/externalLinks").accept(HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.externallinksrelations[0].url").value("http://test/1"));
    }

    @Test
    public void testRetrieveAllGroupsRelationsReturnPagedResources() throws Exception {
        Sample group1 = new TestSample("SAMEG1").build();
        Sample group2 = new TestSample("SAMEG2").build();
        when(sampleRepository.findGroups(anyInt(), anyInt())).thenReturn(getTestPagedResourcesSample(10, group1, group2));


        mockMvc.perform(get("/groupsrelations").accept(HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/hal+json;charset=UTF-8"));

    }

    @Test
    @Ignore
    public void shouldReturnOnlyGroups() {
        //TODO test this feature in the end-to-end test
    }

}
