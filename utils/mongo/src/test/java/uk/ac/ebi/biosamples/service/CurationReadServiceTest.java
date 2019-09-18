package uk.ac.ebi.biosamples.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.mongo.model.MongoCurationLink;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationLinkRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationRepository;
import uk.ac.ebi.biosamples.mongo.service.CurationLinkToMongoCurationLinkConverter;
import uk.ac.ebi.biosamples.mongo.service.MongoCurationLinkToCurationLinkConverter;
import uk.ac.ebi.biosamples.mongo.service.MongoCurationToCurationConverter;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CurationReadService.class,
        MongoCurationLinkToCurationLinkConverter.class,
        MongoCurationToCurationConverter.class})
@ActiveProfiles("test")
public class CurationReadServiceTest {

    @MockBean
    private MongoCurationRepository mongoCurationRepository;
    @MockBean
    private MongoCurationLinkRepository mongoCurationLinkRepository;
    @Autowired
    private MongoCurationLinkToCurationLinkConverter mongoCurationLinkToCurationLinkConverter;
    @Autowired
    private MongoCurationToCurationConverter mongoCurationToCurationConverter;
    @Autowired
    CurationReadService curationReadService;


    @Before
    public void setup() {
        List<MongoCurationLink> mongoCurationLinks = convertToMongoCurationList(getCurationLinksForTest());
        Mockito.when(mongoCurationLinkRepository.findBySample(Mockito.anyString(), Mockito.any()))
                .thenReturn(new PageImpl<>(mongoCurationLinks, new PageRequest(0, 10), 1));
    }

    @Test
    public void test_apply_curation_link() {
        Sample originalSample = getSampleForTest();

        Attribute attributePre = Attribute.build("Organism", "9606");
        Attribute attributePost = Attribute.build("Organism", "Homo sapiens", "iri", "unit");
        Curation curation = Curation.build(attributePre, attributePost);
        CurationLink curationLink = CurationLink.build("SAMN001", curation, "self.domain1", Instant.now());

        Sample curatedSample = new CurationReadService().applyCurationLinkToSample(originalSample, curationLink);

        for (Attribute attribute : curatedSample.getAttributes()) {
            if ("Organism".equalsIgnoreCase(attribute.getType())) {
                Assert.assertEquals(attributePost.getValue(), attribute.getValue());
            }
        }
    }

    @Test
    public void applyAllCurationToSample_test_add_new_curation_attribute() {
        Sample originalSample = getSampleForTest();
        Sample curatedSample = curationReadService.applyAllCurationToSample(originalSample, Optional.empty());

        if (!curatedSample.getAttributes().contains(Attribute.build("NewCuration", "new value", "iri", "unit"))) {
            Assert.fail("Failed to add new curation attribute to sample");
        }

        if (!curatedSample.getExternalReferences().contains(ExternalReference.build("www.ebi.ac.uk/test/new"))) {
            Assert.fail("Failed to add new curation external reference to sample");
        }
    }


    @Test
    public void applyAllCurationToSample_test_delete_curation_attribute() {
        Sample originalSample = getSampleForTest();
        Sample curatedSample = curationReadService.applyAllCurationToSample(originalSample, Optional.empty());

        if (curatedSample.getAttributes().contains(Attribute.build("Weird", "weired value"))) {
            Assert.fail("Failed to delete curation attribute to sample");
        }

        if (curatedSample.getExternalReferences().contains(ExternalReference.build("www.ebi.ac.uk/test/delete"))) {
            Assert.fail("Failed to delete curation external reference to sample");
        }
    }


    @Test
    public void applyAllCurationToSample_test_correct_curation_attribute_order() {
        Sample originalSample = getSampleForTest();
        Sample curatedSample = curationReadService.applyAllCurationToSample(originalSample, Optional.empty());

        for (Attribute attribute : curatedSample.getAttributes()) {
            if ("Organism".equalsIgnoreCase(attribute.getType())) {
                Assert.assertEquals("Bos taurus", attribute.getValue());
            } else if ("CurationDomain".equalsIgnoreCase(attribute.getType())) {
                Assert.assertEquals("domain-b", attribute.getValue());
            }
        }

        if (!curatedSample.getExternalReferences().contains(ExternalReference.build("www.ebi.ac.uk/test/correct"))) {
            Assert.fail("Failed to add external reference curation in correct order");
        }
    }


    private List<CurationLink> getCurationLinksForTest() {
        List<CurationLink> curationLinks = new ArrayList<>();

        Attribute attributePre = Attribute.build("Organism", "9606");
        Attribute attributePost = Attribute.build("Organism", "Homo sapiens", "iri", "unit");
        Curation curation = Curation.build(attributePre, attributePost);
        CurationLink curationLink = CurationLink.build("SAMN001", curation, "self.domain", Instant.now());
        curationLinks.add(curationLink);

        attributePre = Attribute.build("Organism", "Homo sapiens", "iri", "unit");
        attributePost = Attribute.build("organism", "Bos taurus", "iri", "unit");
        curation = Curation.build(attributePre, attributePost);
        curationLink = CurationLink.build("SAMN001", curation, "self.domain", Instant.now().plusSeconds(5));
        curationLinks.add(curationLink);

        attributePre = Attribute.build("Organism", "Homo sapiens", "iri", "unit");
        attributePost = Attribute.build("organism", "should not be this", "iri", "unit");
        curation = Curation.build(attributePre, attributePost);
        curationLink = CurationLink.build("SAMN001", curation, "self.domain", Instant.now().plusSeconds(7));
        curationLinks.add(curationLink);

        attributePre = Attribute.build("Weird", "weired value");
        attributePost = null;
        curation = Curation.build(attributePre, attributePost);
        curationLink = CurationLink.build("SAMN001", curation, "self.domain", Instant.now().plusSeconds(10));
        curationLinks.add(curationLink);

        attributePre = null;
        attributePost = Attribute.build("NewCuration", "new value", "iri", "unit");
        curation = Curation.build(attributePre, attributePost);
        curationLink = CurationLink.build("SAMN001", curation, "self.domain", Instant.now().plusSeconds(15));
        curationLinks.add(curationLink);

        attributePre = Attribute.build("CurationDomain", "domain-a");
        attributePost = Attribute.build("CurationDomain", "domain-b");
        curation = Curation.build(attributePre, attributePost);
        curationLink = CurationLink.build("SAMN001", curation, "self.domain", Instant.now().plusSeconds(20));
        curationLinks.add(curationLink);

        attributePre = Attribute.build("CurationDomain", "domain-a");
        attributePost = Attribute.build("CurationDomain", "domain-c");
        curation = Curation.build(attributePre, attributePost);
        curationLink = CurationLink.build("SAMN001", curation, "self.domain", Instant.now().plusSeconds(25));
        curationLinks.add(curationLink);

        ExternalReference externalReferencePre = ExternalReference.build("www.ebi.ac.uk/test/1");
        ExternalReference externalReferencePost = ExternalReference.build("www.ebi.ac.uk/test/a");
        curation = Curation.build(null, null, Collections.singletonList(externalReferencePre), Collections.singletonList(externalReferencePost));
        curationLink = CurationLink.build("SAMN001", curation, "self.domain", Instant.now().plusSeconds(40));
        curationLinks.add(curationLink);

        externalReferencePre = ExternalReference.build("www.ebi.ac.uk/test/a");
        externalReferencePost = ExternalReference.build("www.ebi.ac.uk/test/correct");
        curation = Curation.build(null, null, Collections.singletonList(externalReferencePre), Collections.singletonList(externalReferencePost));
        curationLink = CurationLink.build("SAMN001", curation, "self.domain", Instant.now().plusSeconds(45));
        curationLinks.add(curationLink);

        externalReferencePre = ExternalReference.build("www.ebi.ac.uk/test/1");
        externalReferencePost = ExternalReference.build("www.ebi.ac.uk/test/wrong");
        curation = Curation.build(null, null, Collections.singletonList(externalReferencePre), Collections.singletonList(externalReferencePost));
        curationLink = CurationLink.build("SAMN001", curation, "self.domain", Instant.now().plusSeconds(50));
        curationLinks.add(curationLink);

        externalReferencePost = ExternalReference.build("www.ebi.ac.uk/test/new");
        curation = Curation.build(null, null, null, Collections.singletonList(externalReferencePost));
        curationLink = CurationLink.build("SAMN001", curation, "self.domain", Instant.now().plusSeconds(55));
        curationLinks.add(curationLink);

        externalReferencePre = ExternalReference.build("www.ebi.ac.uk/test/delete");
        curation = Curation.build(null, null, Collections.singletonList(externalReferencePre), null);
        curationLink = CurationLink.build("SAMN001", curation, "self.domain", Instant.now().plusSeconds(60));
        curationLinks.add(curationLink);

        return curationLinks;
    }

    private Sample getSampleForTest() {
        Set<Attribute> attributes = new HashSet<>();
        Set<Relationship> relationships = new HashSet<>();
        Set<ExternalReference> externalReferences = new HashSet<>();

        attributes.add(Attribute.build("Organism", "9606"));
        attributes.add(Attribute.build("Weird", "weired value"));
        attributes.add(Attribute.build("CurationDomain", "domain-a"));

        externalReferences.add(ExternalReference.build("www.ebi.ac.uk/test/1"));
        externalReferences.add(ExternalReference.build("www.ebi.ac.uk/test/2"));

        return Sample.build("SAMN0001_NAME", "SAMN0001", "self.TestDomain",
                Instant.now(), Instant.now(), attributes, relationships, externalReferences, SubmittedViaType.JSON_API);
    }

    private List<MongoCurationLink> convertToMongoCurationList(List<CurationLink> curations) {
        return curations.stream()
                .map(c -> new CurationLinkToMongoCurationLinkConverter().convert(c))
                .collect(Collectors.toList());
    }

}
