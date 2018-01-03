package uk.ac.ebi.biosamples.legacy.json.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Contact;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Organization;
import uk.ac.ebi.biosamples.model.Publication;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@RunWith(SpringRunner.class)
@SpringBootTest(classes={JSONSampleToSampleConverter.class})
public class LegacyJSONDeserializationTest {

    private Logger log = LoggerFactory.getLogger(getClass());
    String sampleJson;
    String newSampleJson;
    String groupJson;
    Sample legacySample;
    Sample legacyGroup;

    @Autowired
    JSONSampleToSampleConverter sampleConverter;

    @Before
    public void setup() throws IOException {
        sampleJson = Resources.toString(Resources.getResource("testSample.json"), Charsets.UTF_8);
        groupJson = Resources.toString(Resources.getResource("testGroup.json"), Charsets.UTF_8);
        newSampleJson = Resources.toString(Resources.getResource("testSample1.json"), Charsets.UTF_8);

        legacySample = sampleConverter.convert(sampleJson);
    }

    @Test
    public void testConvertAccession() {
        assertThat(legacySample.getAccession()).isEqualTo("SAMEA104493031");
    }

    @Test
    public void testConvertName() {
        assertThat(legacySample.getName()).isEqualTo("source Dex1_Etop10_CM_11");
    }

    @Test
    public void testConvertDates() {
        LocalDate updateDate = legacySample.getUpdate().atZone(ZoneId.of("UTC")).toLocalDate();
        assertThat(updateDate.getYear()).isEqualTo(2017);
        assertThat(updateDate.getMonth()).isEqualTo(Month.NOVEMBER);
        assertThat(updateDate.getDayOfMonth()).isEqualTo(19);

        LocalDate releaseDate = legacySample.getRelease().atZone(ZoneId.of("UTC")).toLocalDate();
        assertThat(releaseDate.getYear()).isEqualTo(2017);
        assertThat(releaseDate.getMonth()).isEqualTo(Month.AUGUST);
        assertThat(releaseDate.getDayOfMonth()).isEqualTo(30);
    }

    @Test
    public void testConvertDescriptionToAttribute() {
        assertThat(legacySample.getAttributes()).contains(Attribute.build("description", "This is a simple description"));
    }

    @Test
    public void testHasRightNumberOfAttributes() {
        List<String> attributes = Arrays.asList("description",
                "organism", "diseaseState", "cellLine", "submission_acc", "submission_title");
        assertThat(legacySample.getAttributes().size()).isEqualTo(attributes.size());
    }

    @Test
    public void testContainsCertainAttributes() {
        assertThat(legacySample.getAttributes().stream().map(Attribute::getType).collect(Collectors.toList()))
                .containsAll(Arrays.asList("organism", "diseaseState", "cellLine"));
    }

    @Test
    public void testAttributeContainsOntologyTerms() {
        List<Attribute> organismAttribute = legacySample.getAttributes().stream().filter(att->att.getType().equals("organism")).collect(Collectors.toList());
        assertThat(organismAttribute).hasSize(1);
        assertThat(organismAttribute.get(0).getIri()).containsOnlyOnce("http://purl.obolibrary.org/obo/NCBITaxon_9606");

    }

    @Test
    public void testConvertContact() {
        assertThat(legacySample.getContacts()).containsOnly(
                new Contact.Builder().name("Leo Zeef").build()
        );
    }

    @Test
    public void testConvertOrganization() {
        assertThat(legacySample.getOrganizations()).containsOnly(
                new Organization.Builder().name("Vanderbilt University")
                .role("submitter").build());
    }


    @Test
    public void testConvertPublications() {
        assertThat(legacySample.getPublications()).containsOnly(
                new Publication.Builder().doi("doi:10.1091/mbc.E11-05-0426")
                .pubmed_id("21680712").build()
        );
    }

    @Test
    public void testConvertSubmissionDataToAttributes() {
        assertThat(legacySample.getAttributes()).contains(
                Attribute.build("submission_acc", "GAE-GEOD-27899"),
                Attribute.build("submission_title", "Methylation profiling in Ulcerative colitis")
        );
    }

    @Test
    public void testConvertEmbeddedExternalReferences() {
        assertThat(legacySample.getExternalReferences()).contains(
                ExternalReference.build("http://www.ebi.ac.uk/ena/data/view/ERP006121")
        );
    }

    @Test
    public void testConvertGroupSamplesIntoRelationships() {
        legacyGroup = sampleConverter.convert(groupJson);
        assertThat(legacyGroup.getRelationships()).contains(
                Relationship.build("SAMEG316628", "has member", "SAMEA4562408")
        );
    }

    @Test
    public void testConvertNewSampleExternalReferences() {
        Sample newSample = sampleConverter.convert(newSampleJson);
    }
}
