package uk.ac.ebi.biosamples;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.JsonLDSample;
import uk.ac.ebi.biosamples.model.Sample;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

//@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Profile({"default", "selenium", "test"})
public class JsonLdIntegration extends AbstractIntegration {
    private final RestOperations restTemplate;
    private final WebDriver chromeDriver = new ChromeDriver();
    private final IntegrationProperties integrationProperties;

    public JsonLdIntegration(RestTemplateBuilder templateBuilder,
                             BioSamplesClient client,
                             IntegrationProperties props) {
        super(client);
        integrationProperties = props;
        restTemplate = templateBuilder.build();
    }

    @Override
    protected void phaseOne() {
        Sample testSample = getTestSample();

        Optional<Resource<Sample>> optionalSample = client.fetchSampleResource(testSample.getAccession());
        if(optionalSample.isPresent()) {
            throw new RuntimeException("JsonLD test sample should not be available during phase 1");
        }
        Resource<Sample> resource = client.persistSampleResource(testSample);
        if (!testSample.equals(resource.getContent())) {
            throw new RuntimeException("Expected response to equal submission");
        }
    }

    @Override
    protected void phaseTwo() {
        Sample testSample = getTestSample();
        checkPresenceOnWebPage(testSample);
        checkPresenceWithRest(testSample);

    }


    @Override
    protected void phaseThree() {

    }

    @Override
    protected void phaseFour() {

    }

    @Override
    protected void phaseFive() {

    }

    private boolean jsonLDIsEmpty(String jsonLDContent) {
        return jsonLDContent.matches("\\{\\s+}");
    }

    private Sample getTestSample() {
        String accession = "SAMEA99332211";
        String name = "Test ld+json";
        LocalDateTime releaseDate = LocalDateTime.now().minusDays(5);
        LocalDateTime updateDate  = LocalDateTime.now();

        SortedSet<Attribute> attributes = new TreeSet<>();
        attributes.add(
                Attribute.build("Organism Part","Lung", "http://purl.obolibrary.org/obo/UBERON_0002048", null)
        );
        attributes.add(
                Attribute.build("test_Type", "test_value")
        );
        attributes.add(
                Attribute.build("Description", "Test description")
        );
        SortedSet<ExternalReference> externalReferences = new TreeSet<>();
        externalReferences.add(
                ExternalReference.build("www.google.com")
        );
        return Sample.build(name,accession,releaseDate,updateDate,
                attributes,null,externalReferences);
    }

    private boolean jsonLDHasAccession(String jsonLDContent, String accession) {
        return Pattern.compile("\"identifier\"\\s*:\\s*\"" + accession + "\",").matcher(jsonLDContent).find();
    }

    private void checkPresenceOnWebPage(Sample sample) {
        try {
            Optional<Resource<Sample>> optionalSample = client.fetchSampleResource(sample.getAccession());
            if (optionalSample.isPresent()) {
                Resource<Sample> sampleResource = optionalSample.get();
                String sampleAccession = sampleResource.getContent().getAccession();
                UriComponents sampleURI = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUri()).pathSegment("samples", sampleAccession).build();
                chromeDriver.get(sampleURI.toString());
                WebElement jsonLDScript = chromeDriver.findElement(By.cssSelector("script[type='application/ld+json']"));
                if (jsonLDScript == null) {
                    throw new RuntimeException("The ld+json script has not been found in the page for sample " + sampleAccession);
                }
                String jsonLDContent = jsonLDScript.getAttribute("innerHTML");
                if (jsonLDIsEmpty(jsonLDContent)) {
                    throw new RuntimeException("The ld+json content is empty");
                }
                if (!jsonLDHasAccession(jsonLDContent,sample.getAccession())) {
                    throw new RuntimeException("The ld+json doesn't have the correct identifier");
                }
            } else {
                throw new RuntimeException("No sample has been found to check for jsonld content");
            }
        } finally {
            chromeDriver.close();
        }

    }

    private void checkPresenceWithRest(Sample sample) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(this.integrationProperties.getBiosampleSubmissionUri());
        uriBuilder.pathSegment("samples", sample.getAccession()+".ldjson");
        ResponseEntity<JsonLDSample> responseEntity = restTemplate.getForEntity(uriBuilder.toUriString(), JsonLDSample.class);
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Error retrieving sample in application/ld+json format from the webapp");
        }
        JsonLDSample jsonLDSample = responseEntity.getBody();
        assert jsonLDSample.getIdentifier().equals(sample.getAccession());

    }
}
