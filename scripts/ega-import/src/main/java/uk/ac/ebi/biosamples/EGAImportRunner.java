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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Component
public class EGAImportRunner implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(EGAImportRunner.class);

    private final BioSamplesClient bioSamplesClient;

    @Autowired
    public EGAImportRunner(BioSamplesClient bioSamplesClient) {
        this.bioSamplesClient = bioSamplesClient;
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
        final String egaUrl = "https://www.ebi.ac.uk/ega/datasets/";
        final ObjectMapper jsonMapper = new ObjectMapper();


        Map<String, SortedSet<String>> datasetToDuoCodesMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(datasetDuoUrl))) {
            String line = br.readLine(); //ignore header
            LOG.info("Reading file: {}, headers: {}", datasetDuoUrl, line);
            while ((line = br.readLine()) != null && !line.isEmpty()) {
                String[] record = line.replaceAll("[\"\\[\\] ]", "").split(",");
                String datasetId = record[0];
                String[] duoCodes = Arrays.copyOfRange(record, 1, record.length);

                datasetToDuoCodesMap.put(datasetId, new TreeSet<>(Arrays.asList(duoCodes)));
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

                    Sample updatedSample = Sample.Builder.fromSample(sample)
                            .addAttribute(Attribute.build("phenotype", phenotype))
                            .addAttribute(Attribute.build("ega dataset id", datasetId))
                            .addAttribute(Attribute.build("ega sample id", egaId))
                            .addExternalReference(ExternalReference.build(egaUrl + datasetId, duoCodes))
                            .withRelease(Instant.now())
                            .build();

                    LOG.info("Original sample: {}", jsonMapper.writeValueAsString(sample));
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
}
