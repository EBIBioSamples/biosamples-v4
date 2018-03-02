package uk.ac.ebi.biosamples.docs;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import uk.ac.ebi.biosamples.model.*;

import java.time.Instant;
import java.util.*;

public class DocumentationHelper {

    private final String[] sampleAccessionPrefix = {"SAME", "SAMD", "SAMEA", "SAMN"};
    private final int maxRandomNumber = 100000;


    private final Random randomGenerator = new Random(new Date().toInstant().toEpochMilli());

    public List<Sample> generateRandomSamples(int numberOfSamples) {

        List<Sample> samples = new ArrayList<>();
        Set<String> usedAccession = new HashSet<>();
        String sampleAccession = null;

        for (int i = 0; i < numberOfSamples; i++) {

            while(sampleAccession == null || usedAccession.contains(sampleAccession)) {

                int randomInt = randomGenerator.nextInt(maxRandomNumber);
                String randomPrefix = sampleAccessionPrefix[randomInt % sampleAccessionPrefix.length];
                sampleAccession = String.format("%s%06d", randomPrefix, randomInt);
            }

            usedAccession.add(sampleAccession);
            Sample sample = new Sample.Builder(sampleAccession, "FakeSample").build();

            samples.add(sample);
        }

        return samples;
    }

    public Sample generateRandomSample() {
        return this.generateRandomSamples(1).get(0);
    }

    public String generateRandomDomain() {
        List<String> domainNames = Arrays.asList("self.BioSamples", "self.USI", "self.ENA", "self.ArrayExpress", "self.EVA", "self.FAANG", "self.HipSci", "self.EBiSC");

        return domainNames.get(randomGenerator.nextInt(domainNames.size()));
    }

    public String generateTestDomain() {
        return "self.DocumentationDomain";
    }

    public Sample.Builder getBuilderFromSample(Sample sample) {
        Sample.Builder sampleBuilder = new Sample.Builder(sample.getAccession(), sample.getName())
                .withDomain(sample.getDomain())
                .withReleaseDate(sample.getRelease())
                .withUpdateDate(sample.getUpdate());

        sample.getAttributes().forEach(sampleBuilder::withAttribute);
        sample.getRelationships().forEach(sampleBuilder::withRelationship);
        sample.getContacts().forEach(sampleBuilder::withContact);
        sample.getPublications().forEach(sampleBuilder::withPublication);
        sample.getOrganizations().forEach(sampleBuilder::withOrganization);

        return sampleBuilder;


    }

    public CurationLink getExampleCurationLinkObject() {

        Curation curationObject = Curation.build(
                Collections.singletonList(Attribute.build("Organism", "Human", "9606", null)),
                Collections.singletonList(Attribute.build("Organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null)),
                Collections.singletonList(ExternalReference.build("www.google.com")),
                Collections.singletonList(ExternalReference.build("www.ebi.ac.uk/ena/ERA123456"))
        );

        CurationLink curationLinkObject = CurationLink.build("SAMEA123456", curationObject, generateTestDomain(), Instant.now());
        return curationLinkObject;
    }


}
