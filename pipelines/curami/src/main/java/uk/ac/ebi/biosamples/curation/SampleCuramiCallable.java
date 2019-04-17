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

public class SampleCuramiCallable implements Callable<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(CuramiApplicationRunner.class);
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
    public Void call() {
        getRuleBasedCurations(sample);
        return null;
    }

    private List<Curation> getRuleBasedCurations(Sample sample) {
        SortedSet<Attribute> attributes = sample.getAttributes();
        List<Curation> curations = new ArrayList<>();
        for (Attribute a : attributes) {
            if (curationRules.containsKey(a.getType())) {
                Curation curation = Curation.build(
                        Attribute.build(a.getType(), a.getValue(), a.getIri(), a.getUnit()),
                        Attribute.build(curationRules.get(a.getType()), a.getValue(), a.getIri(), a.getUnit()));
                bioSamplesClient.persistCuration(sample.getAccession(), curation, domain);
                LOG.info("New curation found {}", curation);
                curations.add(curation);
            }
        }

        return curations;
    }
}
