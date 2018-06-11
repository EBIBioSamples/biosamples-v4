package uk.ac.ebi.biosamples;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

//import org.openqa.selenium.By;
//import org.openqa.selenium.WebDriver;
//import org.openqa.selenium.WebElement;
//import org.openqa.selenium.chrome.ChromeDriver;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Profile({"default", "selenium"})
public class JsonLdIntegration extends AbstractIntegration {
    private final Environment env;
    private final RestOperations restTemplate;
//    private WebDriver chromeDriver;
    private final IntegrationProperties integrationProperties;
    private final BioSamplesProperties bioSamplesProperties;

    public JsonLdIntegration(RestTemplateBuilder templateBuilder,
                             BioSamplesClient client,
                             IntegrationProperties integrationProperties,
                             BioSamplesProperties bioSamplesProperties,
                             Environment env) {
        super(client);
        this.integrationProperties = integrationProperties;
        this.restTemplate = templateBuilder.build();
        this.env = env;
        this.bioSamplesProperties = bioSamplesProperties;

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
			throw new RuntimeException("Expected response ("+resource.getContent()+") to equal submission ("+testSample+")");
        }
    }

    @Override
    protected void phaseTwo() {
        Sample testSample = getTestSample();
        // Check if selenium profile is activate
        if(isSeleniumTestRequired(env)) {
//            checkPresenceOnWebPage(testSample);
        }
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
        String domain = "self.BiosampleIntegrationTest";
		Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
		Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

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
        attributes.add(
                Attribute.build(
                        "MultiCategoryCodeField",
                        "heart and lung",
                        Arrays.asList(
                                "http://purl.obolibrary.org/obo/UBERON_0002048",
                                "http://purl.obolibrary.org/obo/UBERON_0002045",
                                "UBERON:0002045"),
                        null
                )
        );

        SortedSet<ExternalReference> externalReferences = new TreeSet<>();
        externalReferences.add(
                ExternalReference.build("www.google.com")
        );
        return Sample.build(name, accession, domain, release, update,
                attributes,null,externalReferences, null, null, null);
    }

    private boolean jsonLDHasAccession(String jsonLDContent, String accession) {
        return Pattern.compile("\"identifier\"\\s*:\\s*\"" + accession + "\",").matcher(jsonLDContent).find();
    }

    /*
    private void checkPresenceOnWebPage(Sample sample) {
        try {
            this.chromeDriver = new ChromeDriver();
            Optional<Resource<Sample>> optionalSample = client.fetchSampleResource(sample.getAccession());
            if (optionalSample.isPresent()) {
                Resource<Sample> sampleResource = optionalSample.get();
                String sampleAccession = sampleResource.getValue().getAccession();
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
    */

    private void checkPresenceWithRest(Sample sample) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(bioSamplesProperties.getBiosamplesClientUri());
        uriBuilder.pathSegment("samples", sample.getAccession()+".ldjson");
        ResponseEntity<JsonLDDataRecord> responseEntity = restTemplate.getForEntity(uriBuilder.toUriString(), JsonLDDataRecord.class);
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Error retrieving sample in application/ld+json format from the webapp");
        }
        JsonLDDataRecord jsonLDDataRecord = responseEntity.getBody();
        JsonLDSample jsonLDSample = jsonLDDataRecord.getMainEntity();
        assert Stream.of(jsonLDSample.getIdentifiers()).anyMatch(s -> s.equals("biosamples:" + sample.getAttributes()));

        String checkingUrl = UriComponentsBuilder.fromUri(bioSamplesProperties.getBiosamplesClientUri())
                .pathSegment("samples", sample.getAccession()).toUriString();
        assert jsonLDSample.getUrl().equals(checkingUrl);

    }

    private boolean isSeleniumTestRequired(Environment env) {
        return Stream.of(env.getActiveProfiles()).anyMatch(value -> value.matches("selenium"));
    }


}
