package uk.ac.ebi.biosamples;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.ols.OlsProcessor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class EGAImportRunner implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(EGAImportRunner.class);
    private static final String EGA_DATASET_BASE_URL = "https://ega-archive.org/datasets/";
    private static final String EGA_SAMPLE_BASE_URL = "https://ega-archive.org/metadata/v2/samples/";

    private final BioSamplesClient bioSamplesClient;
    private final OlsProcessor olsProcessor;

    @Autowired
    public EGAImportRunner(BioSamplesClient bioSamplesClient, OlsProcessor olsProcessor) {
        this.bioSamplesClient = bioSamplesClient;
        this.olsProcessor = olsProcessor;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (args.getSourceArgs().length < 1) {
            LOG.error("Please specify a data folder as a program argument");
            throw new IllegalArgumentException("Please specify a data folder as a program argument");
        }

//        final String dataFolderUrl = "/home/isuru/BioSamples/EGA_Import/";
        final String dataFolderUrl = args.getSourceArgs()[0];
        final String datasetDuoUrl = dataFolderUrl + "datasets_duo.csv";
        final String sampleDataUrl = dataFolderUrl + "sanger_released_samples.csv";
        final ObjectMapper jsonMapper = new ObjectMapper();


        Map<String, SortedSet<String>> datasetToDuoCodesMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(datasetDuoUrl))) {
            String line = br.readLine(); //ignore header
            LOG.info("Reading file: {}, headers: {}", datasetDuoUrl, line);
            while ((line = br.readLine()) != null && !line.isEmpty()) {
                String[] record = line.replaceAll("[\"\\[\\] ]", "").split(",");
                String datasetId = record[0];
                String[] duoCodes = Arrays.copyOfRange(record, 1, record.length);

                datasetToDuoCodesMap.put(datasetId,
                        new TreeSet<>(Arrays.stream(duoCodes).map(s -> "DUO:" + s).collect(Collectors.toList())));
            }

        } catch (IOException e) {
            LOG.error("couldn't read file: " + datasetDuoUrl, e);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(sampleDataUrl))) {
            String line = br.readLine(); //ignore header
            LOG.info("Reading file: {}, headers: {}", sampleDataUrl, line);
            while ((line = br.readLine()) != null && !line.isEmpty()) {
                String[] sampleValues = line.split(",");
                String accession = sampleValues[0];
                String egaId = sampleValues[1];
                String datasetId = sampleValues[2];
                String phenotype = sampleValues[3];
                SortedSet<String> duoCodes = datasetToDuoCodesMap.get(datasetId);

                Optional<Resource<Sample>> sampleResource = bioSamplesClient.fetchSampleResource(accession);
                if (sampleResource.isPresent()) {
                    Sample sample = sampleResource.get().getContent();
                    LOG.info("Original sample: {}", jsonMapper.writeValueAsString(sample));
                    if (sample.getAttributes().size() != 2) {
                        LOG.info("Attributes size != 2, Attributes {}", sample.getAttributes());
                    }

                    //remove extra attributes from migration (deleted and other-migrated from....)
                    List<Attribute> attributesToRemove = new ArrayList<>();
                    for (Attribute attribute : sample.getAttributes()) {
                        if (attribute.getType().equals("deleted") ||
                                (attribute.getType().equals("other") && attribute.getValue().startsWith("migrated from"))) {
                            attributesToRemove.add(attribute);
                        } else if (attribute.getType().equals("phenotype")) {
                            LOG.warn("Removing attribute phenotype={} from original sample", attribute.getValue());
                            attributesToRemove.add(attribute);
                        }
                    }
                    for (Attribute attribute : attributesToRemove) {
                        sample.getAttributes().remove(attribute);
                    }

                    /*Optional<String> olsPhenotype = getOlsMappedTerm(phenotype);
                    Attribute attributePhenotype;
                    if (olsPhenotype.isPresent()) {
                        attributePhenotype = Attribute.build("phenotype", phenotype, olsPhenotype.get(), null);
                    } else {
                        attributePhenotype = Attribute.build("phenotype", phenotype);
                    }*/

                    Optional<OlsProcessor.OlsResult> olsPhenotype = getOlsMappedTerm(phenotype);
                    Attribute attributePhenotype;
                    if (olsPhenotype.isPresent()) {
                        attributePhenotype = Attribute.build("phenotype", olsPhenotype.get().getLabel(), olsPhenotype.get().getIri(), null);
                    } else {
                        attributePhenotype = Attribute.build("phenotype", phenotype);
                    }

                    Sample updatedSample = Sample.Builder.fromSample(sample)
                            .addAttribute(attributePhenotype)
                            .addAttribute(Attribute.build("ega dataset id", datasetId))
                            .addAttribute(Attribute.build("ega sample id", egaId))
                            .addExternalReference(ExternalReference.build(EGA_DATASET_BASE_URL + datasetId, duoCodes))
                            .addExternalReference(ExternalReference.build(EGA_SAMPLE_BASE_URL + egaId))
                            .withRelease(Instant.now())
                            .build();

                    LOG.info("Updated sample: {}", jsonMapper.writeValueAsString(updatedSample));
                    bioSamplesClient.persistSampleResource(updatedSample);
                } else {
                    LOG.warn("Sample not present in biosamples: {}", accession);
                }
            }

        } catch (JsonProcessingException e) {
            LOG.error("JSON conversion failure", e);
        } catch (IOException e) {
            LOG.error("Couldn't read file: " + datasetDuoUrl, e);
        }
    }


//    private Optional<String> getOlsMappedTerm(String termToMap) {
//        Optional<String> olsMappedTerm = Optional.empty();
//        if (termToMap.matches("^[A-Za-z]+[_:\\-][0-9]+$")) {
//            olsMappedTerm = olsProcessor.queryOlsForShortcode(termToMap);
//            olsMappedTerm.ifPresent(o -> LOG.info("OLS mapped term {} into {}", termToMap, o));
//        }
//
//        return olsMappedTerm;
//    }


    private Optional<OlsProcessor.OlsResult> getOlsMappedTerm(String termToMap) {
        Optional<OlsProcessor.OlsResult> olsMappedTerm = Optional.empty();
        if (termToMap.matches("^[A-Za-z]+[_:\\-][0-9]+$")) {
            olsMappedTerm = olsProcessor.queryForOlsObject(termToMap);
            olsMappedTerm.ifPresent(o -> LOG.info("OLS mapped term {} into {}", termToMap, o.getIri()));
        }

        return olsMappedTerm;
    }
}
