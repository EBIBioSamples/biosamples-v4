/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.legacy.json.controller;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.hateoas.MediaTypes.HAL_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import uk.ac.ebi.biosamples.legacy.json.domain.GroupsRelations;
import uk.ac.ebi.biosamples.legacy.json.domain.TestAttribute;
import uk.ac.ebi.biosamples.legacy.json.domain.TestSample;
import uk.ac.ebi.biosamples.legacy.json.repository.RelationsRepository;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Contact;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Organization;
import uk.ac.ebi.biosamples.model.Publication;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
public class LegacySamplesControllerIntegrationTest {

  @MockBean private SampleRepository sampleRepository;

  @MockBean private RelationsRepository relationsRepository;

  @Autowired private MockMvc mockMvc;

  @Autowired PagedResourcesAssembler<Sample> pagedResourcesAssembler;

  private PagedResources<Resource<Sample>> getTestPagedResourcesSample(
      int totalSamples, Sample... samples) {
    List<Sample> allSamples = Stream.of(samples).collect(Collectors.toList());

    Pageable pageInfo = new PageRequest(0, samples.length);
    Page<Sample> samplePage = new PageImpl<>(allSamples, pageInfo, totalSamples);
    return pagedResourcesAssembler.toResource(samplePage);
  }

  @Test
  public void testReturnSampleByAccession() throws Exception {
    Sample testSample = new TestSample("SAMEA123123").build();
    when(sampleRepository.findByAccession(testSample.getAccession()))
        .thenReturn(Optional.of(testSample));

    mockMvc
        .perform(
            get("/samples/{accession}", testSample.getAccession())
                .accept("application/hal+json;charset=UTF-8"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accession").value("SAMEA123123"));
  }

  @Test
  public void testSampleAttributeArCamelcased() throws Exception {
    Sample testSample =
        new TestSample("SAMEA123123")
            .withAttribute(Attribute.build("Organism Part", "Homo sapiens"))
            .withAttribute(Attribute.build("Sample_source_name", "Generic 1.0"))
            .build();

    when(sampleRepository.findByAccession(testSample.getAccession()))
        .thenReturn(Optional.of(testSample));

    mockMvc
        .perform(
            get("/samples/{accession}", testSample.getAccession())
                .accept("application/hal+json;charset=UTF-8"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.characteristics.organismPart[0].text").value("Homo sapiens"))
        .andExpect(jsonPath("$.characteristics.sampleSourceName[0].text").value("Generic 1.0"));
  }

  @Test
  public void testResponseContentTypeIsHalJson() throws Exception {
    Sample testSample = new TestSample("SAMEA0").build();
    when(sampleRepository.findByAccession(testSample.getAccession()))
        .thenReturn(Optional.of(testSample));

    mockMvc
        .perform(get("/samples/SAMEA0").accept("application/hal+json;charset=UTF-8"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json;charset=UTF-8"));
  }

  @Test
  public void testSampleDescriptionIsExposedAsRootAttribute() throws Exception {
    Sample testSample =
        new TestSample("SAMEA1")
            .withAttribute(new TestAttribute("description", "simple description").build())
            .build();
    when(sampleRepository.findByAccession(testSample.getAccession()))
        .thenReturn(Optional.of(testSample));

    mockMvc
        .perform(get("/samples/{accession}", "SAMEA1").accept(HAL_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("simple description"));
  }

  @Test
  public void testSampleReleaseDateFormatAsLocalDate() throws Exception {

    Sample testSample =
        new TestSample("SAMEA1").releasedOn(Instant.parse("2016-01-01T00:30:00Z")).build();
    when(sampleRepository.findByAccession(testSample.getAccession()))
        .thenReturn(Optional.of(testSample));

    mockMvc
        .perform(get("/samples/SAMEA1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.releaseDate").value("2016-01-01"));
  }

  @Test
  public void testSampleOntologyURIsIsAlwaysAnArray() throws Exception {

    Sample testSample =
        new TestSample("SAMEA0")
            .withAttribute(new TestAttribute("type", "value").withOntologyUri("test").build())
            .build();

    when(sampleRepository.findByAccession(testSample.getAccession()))
        .thenReturn(Optional.of(testSample));

    mockMvc
        .perform(get("/samples/SAMEA0").accept(HAL_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.characteristics.type[0].ontologyTerms").isArray())
        .andExpect(jsonPath("$.characteristics.type[0].ontologyTerms[0]").value("test"));
  }

  @Test
  public void testSampleAttributesAreCamelCasedAndPurified() throws Exception {
    Sample testSample =
        new TestSample("SAME0")
            .withAttribute(new TestAttribute("This is a Strange type (indeed!!!)", "Value").build())
            .build();

    when(sampleRepository.findByAccession(testSample.getAccession()))
        .thenReturn(Optional.of(testSample));

    mockMvc
        .perform(get("/samples/SAME0").accept(HAL_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.characteristics").value(hasKey("thisIsAStrangeTypeIndeed")));
  }

  @Test
  public void testSampleNameIsExposedAsRootField() throws Exception {
    Sample testSample = new TestSample("SAMEA2").withName("StrangeName").build();
    when(sampleRepository.findByAccession(testSample.getAccession()))
        .thenReturn(Optional.of(testSample));

    mockMvc
        .perform(get("/samples/SAMEA2").accept(MediaTypes.HAL_JSON_VALUE))
        .andExpect(jsonPath("$.name").value("StrangeName"));
  }

  @Test
  public void testSampleResourceHasLinksForSampleSelfAndRelationships() throws Exception {
    Sample testSample = new TestSample("SAMN12").build();
    when(sampleRepository.findByAccession(testSample.getAccession()))
        .thenReturn(Optional.of(testSample));

    mockMvc
        .perform(get("/samples/{accession}", testSample.getAccession()).accept(HAL_JSON))
        .andExpect(
            jsonPath("$._links")
                .value(
                    allOf(
                        Matchers.hasKey("sample"),
                        Matchers.hasKey("self"),
                        Matchers.hasKey("relations"))));
  }

  @Test
  public void testSampleResourceLinkForSampleEqualsLinkForSelf() throws Exception {
    Sample testSample = new TestSample("SAMEA555").build();
    when(sampleRepository.findByAccession(testSample.getAccession()))
        .thenReturn(Optional.of(testSample));

    mockMvc
        .perform(get("/samples/{accession}", testSample.getAccession()).accept(HAL_JSON))
        .andDo(
            result -> {
              String responseContent = result.getResponse().getContentAsString();
              String sampleHref = JsonPath.parse(responseContent).read("$._links.sample.href");
              String selfHref = JsonPath.parse(responseContent).read("$._links.self.href");
              assertEquals(sampleHref, selfHref);
            });
  }

  @Test
  public void testUsingRelationsLinkGetsARelationsResource() throws Exception {
    Sample testSample =
        new TestSample("SAMD666")
            .withRelationship(Relationship.build("SAMD666", "deriveFrom", "SAMD555"))
            .build();
    when(sampleRepository.findByAccession(testSample.getAccession()))
        .thenReturn(Optional.of(testSample));

    MvcResult result =
        mockMvc
            .perform(get("/samples/{accession}", testSample.getAccession()).accept(HAL_JSON))
            .andExpect(
                jsonPath("$._links.relations.href")
                    .value(Matchers.endsWith("/samplesrelations/SAMD666")))
            .andReturn();

    String deriveFromLink =
        JsonPath.parse(result.getResponse().getContentAsString()).read("$._links.relations.href");
    mockMvc
        .perform(get(deriveFromLink).accept(HAL_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accession").value("SAMD666"));
  }

  @Test
  @Ignore("Not valid for legacy json")
  public void testMsiInfoShouldNotBeVisibleInTheSample() throws Exception {
    Sample sample =
        new TestSample("SAMEA1")
            .withContact(new Contact.Builder().name("Name").build())
            .withOrganization(new Organization.Builder().role("submitter").name("org").build())
            .withPublication(new Publication.Builder().doi("someDOI").pubmed_id("someID").build())
            .build();
    when(sampleRepository.findByAccession(sample.getAccession())).thenReturn(Optional.of(sample));
    mockMvc
        .perform(get("/samples/{accession}", sample.getAccession()).accept(HAL_JSON))
        .andExpect(
            jsonPath("$")
                .value(
                    not(
                        anyOf(
                            hasKey("contacts"), hasKey("organizations"), hasKey("publications")))));
  }

  @Test
  //	@Ignore("Invalid test for legacy purposes")
  public void testContactShouldNotBeVisibleInSample() throws Exception {
    Sample sample =
        new TestSample("SAMEA1").withContact(new Contact.Builder().name("Name").build()).build();
    when(sampleRepository.findByAccession(sample.getAccession())).thenReturn(Optional.of(sample));

    mockMvc
        .perform(get("/samples/{accession}", sample.getAccession()).accept(HAL_JSON))
        .andExpect(jsonPath("$.contact").isArray())
        .andExpect(jsonPath("$.contact[0]").value(hasKey("Name")));
  }

  @Test
  //	@Ignore("Invalid test for legacy purposes")
  public void testContactMixinHidesUnwantedFields() throws Exception {
    Sample sample =
        new TestSample("SAMEA1")
            .withContact(
                new Contact.Builder()
                    .firstName("first")
                    .midInitials("mi")
                    .lastName("last")
                    .name("real name")
                    .email("me@you.com")
                    .affiliation("nobody inc.")
                    .role("screamer")
                    .build())
            .build();
    when(sampleRepository.findByAccession(sample.getAccession())).thenReturn(Optional.of(sample));

    mockMvc
        .perform(get("/samples/{accession}", sample.getAccession()).accept(HAL_JSON))
        .andExpect(jsonPath("$.contact").isArray())
        .andExpect(
            jsonPath("$.contact[0]")
                .value(
                    not(
                        anyOf(
                            hasKey("FirstName"),
                            hasKey("LastName"),
                            hasKey("MidInitials"),
                            hasKey("Role"),
                            hasKey("URL"),
                            hasKey("E-mail")))));
  }

  @Test
  //	@Ignore("Invalid test for legacy purposes")
  public void testPublicationIsNotAvailableInSample() throws Exception {
    Sample sample =
        new TestSample("SAMEA1")
            .withPublication(new Publication.Builder().doi("doi").pubmed_id("pubmedID").build())
            .build();
    when(sampleRepository.findByAccession(sample.getAccession())).thenReturn(Optional.of(sample));

    mockMvc
        .perform(get("/samples/{accession}", sample.getAccession()).accept(HAL_JSON))
        .andExpect(jsonPath("$.publications").isArray())
        .andExpect(jsonPath("$.publications[0]").value(allOf(hasKey("doi"), hasKey("pubmed_id"))));
  }

  @Test
  @Ignore("Invalid test for legacy purposes")
  public void testOrganizationNotAvailableInSample() throws Exception {

    Sample testSample =
        new TestSample("SAMEA1")
            .withOrganization(
                new Organization.Builder()
                    .name("Stanford Microarray Database (SMD)")
                    .role("submitter")
                    .build())
            .build();
    when(sampleRepository.findByAccession(testSample.getAccession()))
        .thenReturn(Optional.of(testSample));

    mockMvc
        .perform(get("/samples/{accession}", testSample.getAccession()).accept(HAL_JSON))
        .andExpect(jsonPath("$.organization").isArray())
        .andExpect(
            jsonPath("$.organization[0]")
                .value(
                    allOf(
                        hasEntry("Name", "Stanford Microarray Database (SMD)"),
                        hasEntry("Role", "submitter"))));
  }

  @Test
  public void testRetrieveAllSamplesAsPagedResources() throws Exception {
    Sample sampleA = new TestSample("A").build();
    Sample sampleB =
        new TestSample("B")
            .withAttribute(new TestAttribute("Organism", "Homo sapiens").build())
            .build();
    when(sampleRepository.findSamples(anyInt(), anyInt()))
        .thenReturn(getTestPagedResourcesSample(10, sampleA, sampleB));

    mockMvc
        .perform(get("/samples").accept(HAL_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json;charset=UTF-8"))
        .andExpect(jsonPath("$").isMap())
        .andExpect(jsonPath("$..accession").value(containsInAnyOrder("A", "B")))
        .andExpect(
            jsonPath("$._embedded.samples[?(@.accession=='B')].characteristics.organism.*.text")
                .value("Homo sapiens"));
  }

  @Test
  public void testSampleHasExternalReferencesAsRootField() throws Exception {
    Sample sample =
        new TestSample("SAMEA1")
            .withExternalReference(ExternalReference.build("http://hpscreg.eu/cell-lines/PZIF-002"))
            .build();

    when(sampleRepository.findByAccession(sample.getAccession())).thenReturn(Optional.of(sample));

    mockMvc
        .perform(get("/samples/{accession}", sample.getAccession()).accept(HAL_JSON))
        .andExpect(jsonPath("$.externalReferences.*.name").value(containsInAnyOrder("hPSCreg")))
        .andExpect(jsonPath("$.externalReferences[?(@.name=='hPSCreg')].acc").value("PZIF-002"));
  }

  @Test
  public void testAllSamplesLinksContainsSearch() throws Exception {

    Sample sampleA = new TestSample("A").build();
    Sample sampleB =
        new TestSample("B")
            .withAttribute(new TestAttribute("Organism", "Homo sapiens").build())
            .build();
    when(sampleRepository.findSamples(anyInt(), anyInt()))
        .thenReturn(getTestPagedResourcesSample(10, sampleA, sampleB));

    String responseContent =
        mockMvc
            .perform(get("/samples").accept(HAL_JSON))
            .andExpect(jsonPath("$._links.search").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String searchEndpoint = JsonPath.parse(responseContent).read("$._links.search.href");

    mockMvc
        .perform(get(searchEndpoint).accept(HAL_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json;charset=UTF-8"))
        .andExpect(jsonPath("$._embedded").doesNotExist())
        .andExpect(jsonPath("$._links").isNotEmpty())
        .andExpect(
            jsonPath("$._links")
                .value(
                    allOf(
                        hasKey("findFirstByGroupsContains"),
                        hasKey("findByGroups"),
                        hasKey("findByAccession"),
                        hasKey("findByText"),
                        hasKey("findByTextAndGroups"),
                        hasKey("findByAccessionAndGroups"),
                        hasKey("self"))));
  }

  @Test
  public void testFindFirstByGroupFunctionality() throws Exception {
    Sample sampleA = new TestSample("A").build();
    when(sampleRepository.findFirstSampleByGroup("groupA"))
        .thenReturn(Optional.of(new Resource(sampleA)));
    when(sampleRepository.findFirstSampleByGroup("groupB")).thenReturn(Optional.empty());

    String searchLinkContent =
        mockMvc
            .perform(get("/samples/search").accept(HAL_JSON))
            .andExpect(
                jsonPath("$._links.findFirstByGroupsContains.href").value(endsWith("{?group}")))
            .andReturn()
            .getResponse()
            .getContentAsString();

    UriTemplate findFirstByGroupTemplateUrl =
        new UriTemplate(
            JsonPath.parse(searchLinkContent).read("$._links.findFirstByGroupsContains.href"));

    mockMvc
        .perform(get(findFirstByGroupTemplateUrl.expand("groupA")).accept(HAL_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json;charset=UTF-8"))
        .andExpect(jsonPath("$.accession").value("A"));

    mockMvc
        .perform(get(findFirstByGroupTemplateUrl.expand("groupB")).accept(HAL_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  public void testFindByGroupFunctionality() throws Exception {
    Sample sampleA =
        new TestSample("SAME2")
            .withRelationship(Relationship.build("SAMEG1", "has member", "SAME2"))
            .build();
    Sample sampleB =
        new TestSample("SAMN3")
            .withRelationship(Relationship.build("SAMEG1", "has member", "SAMN3"))
            .build();
    Sample groupA =
        new TestSample("SAMEG1")
            .withRelationship(Relationship.build("SAMEG1", "has member", "SAME2"))
            .withRelationship(Relationship.build("SAMEG1", "has member", "SAMN3"))
            .build();

    when(sampleRepository.findSamplesByGroup(eq("SAMEG1"), anyInt(), anyInt()))
        .thenReturn(getTestPagedResourcesSample(2, sampleA, sampleB));
    when(relationsRepository.getGroupsRelationships(anyString()))
        .thenReturn(Collections.singletonList(new GroupsRelations(groupA)));

    String searchLinkContent =
        mockMvc
            .perform(get("/samples/search").accept(HAL_JSON))
            .andExpect(
                jsonPath("$._links.findByGroups.href").value(endsWith("{?group,size,page,sort}")))
            .andReturn()
            .getResponse()
            .getContentAsString();

    UriTemplate findFirstByGroupTemplateUrl =
        new UriTemplate(JsonPath.parse(searchLinkContent).read("$._links.findByGroups.href"));
    Map<String, String> urlParameters = new HashMap<>();
    urlParameters.put("group", "SAMEG1");
    urlParameters.put("page", "0");
    urlParameters.put("size", "50");

    String responseContent =
        mockMvc
            .perform(get(findFirstByGroupTemplateUrl.expand(urlParameters)).accept(HAL_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/hal+json;charset=UTF-8"))
            .andExpect(
                jsonPath("$._embedded.samples.*.accession")
                    .value(containsInAnyOrder("SAME2", "SAMN3")))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String groupRelationLink =
        JsonPath.parse(responseContent).read("$._embedded.samples[0]._links.relations.href")
            + "/groups";
    mockMvc
        .perform(get(groupRelationLink).accept(HAL_JSON))
        .andExpect(
            jsonPath("$._embedded.groupsrelations[0].accession").value(groupA.getAccession()));
  }

  @Test
  public void testFindByAccessionFunctionality() throws Exception {
    /*TODO testFindByAccessionFunctionality*/
    Sample sampleA = new TestSample("A").build();
    when(sampleRepository.findByAccession("A")).thenReturn(Optional.of(sampleA));

    String searchLinkContent =
        mockMvc
            .perform(get("/samples/search").accept(HAL_JSON))
            .andExpect(
                jsonPath("$._links.findByAccession.href")
                    .value(endsWith("{?accession,size,page,sort}")))
            .andReturn()
            .getResponse()
            .getContentAsString();

    UriTemplate findByAccessionUrlTemplate =
        new UriTemplate(JsonPath.parse(searchLinkContent).read("$._links.findByAccession.href"));
    Map<String, String> urlParameters = new HashMap<>();
    urlParameters.put("accession", "A");
    urlParameters.put("page", "0");
    urlParameters.put("size", "50");

    mockMvc
        .perform(get(findByAccessionUrlTemplate.expand(urlParameters)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$").value(allOf(hasKey("_embedded"), hasKey("_links"), hasKey("page"))))
        .andExpect(jsonPath("$._embedded.samples").exists());
  }
}
