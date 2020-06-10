package uk.ac.ebi.biosamples.zooma;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.PipelineResult;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.amr.AMREntry;
import uk.ac.ebi.biosamples.model.structured.amr.AMRTable;
import uk.ac.ebi.biosamples.service.CurationApplicationService;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class SampleZoomaCallable implements Callable<PipelineResult> {
    private Logger log = LoggerFactory.getLogger(getClass());
    private final Sample sample;
    private final BioSamplesClient bioSamplesClient;
    private final ZoomaProcessor zoomaProcessor;
    private final CurationApplicationService curationApplicationService;
    private final String domain;
    private int curationCount;
    public static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<>();

    public SampleZoomaCallable(BioSamplesClient bioSamplesClient, Sample sample,
                               ZoomaProcessor zoomaProcessor,
                               CurationApplicationService curationApplicationService, String domain) {
        this.bioSamplesClient = bioSamplesClient;
        this.sample = sample;
        this.zoomaProcessor = zoomaProcessor;
        this.curationApplicationService = curationApplicationService;
        this.domain = domain;
        this.curationCount = 0;
    }

    @Override
    public PipelineResult call() {
        boolean success = true;
        try {
            Sample last;
            Sample curated = sample;

            do {
                last = curated;
                curated = zooma(last);
            } while (!last.equals(curated));
        } catch (Exception e) {
            log.warn("Encountered exception with " + sample.getAccession(), e);
            failedQueue.add(sample.getAccession());
            success = false;
        }

        return new PipelineResult(sample.getAccession(), curationCount, success);
    }

    private Sample zooma(Sample sample) {
        for (Attribute attribute : sample.getAttributes()) {
            //if there are any iris already, skip zoomafying it and curate elsewhere
            if (attribute.getIri().size() > 0) {
                continue;
            }

            //do nothing - removed a loop as attribute.getIri() is always null

            //if it has units, skip it
            if (attribute.getUnit() != null) {
                continue;
            }

            if (attribute.getType().toLowerCase().equals("synonym")) {
                log.trace("Skipping synonym " + attribute.getValue());
                continue;
            }

            if (attribute.getType().toLowerCase().equals("other")) {
                log.trace("Skipping other " + attribute.getValue());
                continue;
            }

            if (attribute.getType().toLowerCase().equals("unknown")) {
                log.trace("Skipping unknown " + attribute.getValue());
                continue;
            }

            if (attribute.getType().toLowerCase().equals("description")) {
                log.trace("Skipping description " + attribute.getValue());
                continue;
            }

            if (attribute.getType().toLowerCase().equals("label")) {
                log.trace("Skipping label " + attribute.getValue());
                continue;
            }

            if ("model".equals(attribute.getType().toLowerCase())
                    || "package".equals(attribute.getType().toLowerCase())
                    || "INSDC first public".equals(attribute.getType())
                    || "INSDC last update".equals(attribute.getType())
                    || "NCBI submission model".equals(attribute.getType())
                    || "NCBI submission package".equals(attribute.getType())
                    || "INSDC status".equals(attribute.getType())
                    || "ENA checklist".equals(attribute.getType())
                    || "INSDC center name".equals(attribute.getType())) {
                log.trace("Skipping " + attribute.getType() + " : " + attribute.getValue());
                continue;
            }

            if (attribute.getType().toLowerCase().equals("host_subject_id")) {
                log.trace("Skipping host_subject_id " + attribute.getValue());
                continue;
            }

            if (attribute.getValue().matches("^[0-9.-]+$")) {
                log.trace("Skipping number " + attribute.getValue());
                continue;
            }

            if (attribute.getValue().matches("^[ESD]R[SRX][0-9]+$")) {
                log.trace("Skipping SRA/ENA/DDBJ identifier " + attribute.getValue());
                continue;
            }

            if (attribute.getValue().matches("^GSM[0-9]+$")) {
                log.trace("Skipping GEO identifier " + attribute.getValue());
                continue;
            }

            if (attribute.getValue().matches("^SAM[END]A?G?[0-9]+$")) {
                log.trace("Skipping BioSample identifier " + attribute.getValue());
                continue;
            }

            if (attribute.getType().length() < 64 && attribute.getValue().length() < 128) {
                Optional<String> iri = zoomaProcessor.queryZooma(attribute.getType(), attribute.getValue());

                if (iri.isPresent()) {
                    log.trace("Mapped " + attribute + " to " + iri.get());
                    Attribute mapped = Attribute.build(attribute.getType(), attribute.getValue(), attribute.getTag(), iri.get(), null);
                    Curation curation = Curation.build(Collections.singleton(attribute), Collections.singleton(mapped), null, null);

                    //save the curation back in biosamples
                    bioSamplesClient.persistCuration(sample.getAccession(), curation, domain);
                    sample = curationApplicationService.applyCurationToSample(sample, curation);
                    curationCount++;
                }
            }
        }

       /* if (sample.getData() != null && sample.getData().size() > 0) {
            Set<AbstractData> annotatedAmrData = annotateAmr(sample);

            if (annotatedAmrData != null && annotatedAmrData.size() > 0) {
                Sample.Builder.fromSample(sample).withData(annotatedAmrData).build();
                bioSamplesClient.persistSampleResource(sample);
            }
        }*/

        return sample;
    }

    private Set<AbstractData> annotateAmr(Sample sample) {
        AtomicBoolean iriUpdate = new AtomicBoolean(false);

        return sample.getData().stream().map(data -> {
            AMRTable table = null;

            if (data instanceof AMRTable) {
                table = (AMRTable) data;

                Set<AMREntry> amrEntries = table.getStructuredData().stream().map(amrEntry -> {
                    final Optional<String> antibioticIri = zoomaProcessor.queryZooma("", amrEntry.getAntibioticName().getValue());
                    final Optional<String> organismIri = zoomaProcessor.queryZooma("", amrEntry.getSpecies().getValue());

                    if (antibioticIri.isPresent()) {
                        log.trace("Mapped " + amrEntry.getAntibioticName().getValue() + " to " + antibioticIri.get());

                        amrEntry.getAntibioticName().setIri(antibioticIri.get());
                        iriUpdate.set(true);
                    }

                    if (organismIri.isPresent()) {
                        log.trace("Mapped " + amrEntry.getSpecies().getValue() + " to " + organismIri.get());

                        amrEntry.getSpecies().setIri(organismIri.get());
                        iriUpdate.set(true);
                    }

                    if (iriUpdate.get()) {
                        return amrEntry;
                    }

                    return amrEntry;

                }).collect(Collectors.toSet());

                table.getStructuredData().addAll(amrEntries);
            }

            return table;

        }).collect(Collectors.toSet());
    }
}
