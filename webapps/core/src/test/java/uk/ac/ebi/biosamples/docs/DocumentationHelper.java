package uk.ac.ebi.biosamples.docs;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import uk.ac.ebi.biosamples.model.Sample;

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
            Sample sample = new Sample.Builder(sampleAccession, "Fake sample").build();

            samples.add(sample);
        }

        return samples;
    }

}
