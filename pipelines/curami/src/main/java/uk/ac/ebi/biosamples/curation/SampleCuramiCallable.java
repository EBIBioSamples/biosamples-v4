package uk.ac.ebi.biosamples.curation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.Callable;

public class SampleCuramiCallable implements Callable<Integer> {
    private static final Logger LOG = LoggerFactory.getLogger(SampleCuramiCallable.class);

    private final Sample sample;
    private final BioSamplesClient bioSamplesClient;
    private final String domain;
    private final Map<String, String> curationRules;

    public SampleCuramiCallable(BioSamplesClient bioSamplesClient, Sample sample, String domain,
                                Map<String, String> curationRules) {
        this.bioSamplesClient = bioSamplesClient;
        this.sample = sample;
        this.domain = domain;
        this.curationRules = curationRules;
    }

    @Override
    public Integer call() {
        List<Curation> curations = getRuleBasedCurations(sample);
        if (!curations.isEmpty()) {
            LOG.info("{} curations added for sample {}", curations.size(), sample.getAccession());
        }
        return curations.size();
    }

    private List<Curation> getRuleBasedCurations(Sample sample) {
        SortedSet<Attribute> attributes = sample.getAttributes();
        List<Curation> curations = new ArrayList<>();
        for (Attribute a : attributes) {
            String processedAttribute = getCleanedAttribute(a.getType());
            if (curationRules.containsKey(processedAttribute)) {
                Curation curation = Curation.build(
                        Attribute.build(a.getType(), a.getValue(), null, a.getIri(), a.getUnit()),
                        Attribute.build(curationRules.get(processedAttribute), a.getValue(), null, a.getIri(), a.getUnit()));
                bioSamplesClient.persistCuration(sample.getAccession(), curation, domain);
                LOG.info("New curation found {}", curation);
                curations.add(curation);
            }
        }

        return curations;
    }

    //todo add further processing identified in the analysis
    private String getCleanedAttribute(String attribute) {
        /*String cleanedAttribute = attribute.toLowerCase();
        cleanedAttribute = cleanedAttribute.replaceAll("-", " ");


        //camel to snake
        //whitespace around paranthesis
        //Fancy characters same as python analysis
        //

        return cleanedAttribute;*/
        return attribute; // add all transformation as rules
    }
}
