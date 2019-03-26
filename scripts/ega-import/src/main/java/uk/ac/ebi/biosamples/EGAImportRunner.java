package uk.ac.ebi.biosamples;

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

import java.io.*;
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
        String accession = "";
        Optional<Resource<Sample>> sampleResource = bioSamplesClient.fetchSampleResource(accession);
        if (sampleResource.isPresent()) {
            System.out.println(sampleResource.get().getContent());
        } else {
            System.out.println("Cant find the sample");
        }


        /*String dataFolderUrl = "/home/isuru/BioSamples/EGA_Import/";
        String datasetDuoUrl = dataFolderUrl + "datasets_duo.csv";
        String sampleDataUrl = dataFolderUrl + "sanger_released_samples.csv";

        Map<String, List<String>> datasetToDuoCodesMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(datasetDuoUrl))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] record = line.replaceAll("[\"\\[\\]]", "").split(",");
                String datasetId = record[0];
                String[] duoCodes = Arrays.copyOfRange(record, 1, record.length);

                datasetToDuoCodesMap.put(datasetId, Arrays.asList(duoCodes));
            }

        } catch (IOException e) {
            LOG.error("Coulnt read file: {}", datasetDuoUrl);
        }


        try (BufferedReader br = new BufferedReader(new FileReader(sampleDataUrl))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] sampleValues = line.split(",");
                String accession = sampleValues[0];
                String egaId = sampleValues[1];
                String datasetId = sampleValues[2];
                String phenotype = sampleValues[3];
                List<String> duoCodes = datasetToDuoCodesMap.get(datasetId);

                Optional<Resource<Sample>> sampleResource = bioSamplesClient.fetchSampleResource(accession);
                if (sampleResource.isPresent()) {
                    Sample sample = sampleResource.get().getContent();

                    Sample updatedSample = Sample.Builder.fromSample(sample)
                            .addAttribute(Attribute.build("phenotype", phenotype))
                            .addAttribute(Attribute.build("ega_dataset", datasetId))
                            .addExternalReference(ExternalReference.build(egaId))
                            .build();

                    sample.getCharacteristics().add(Attribute.build("phenotype", phenotype));

                } else {
                    LOG.warn("Sample not present in biosamples: {}", accession);
                }
            }

        } catch (IOException e) {
            LOG.error("Coulnt read file: {}", datasetDuoUrl);
        }


        String accession = "SAMEA2062883";
        Optional<Resource<Sample>> sampleResource = bioSamplesClient.fetchSampleResource(accession);
        if (sampleResource.isPresent()) {
            System.out.println(sampleResource.get().getContent());
        } else {
            System.out.println("Cant find the sample");
        }*/


//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("not-founds.txt"))) {
//            long count = 0;
//            long startTime = System.currentTimeMillis();
//
//            for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll("", Collections.emptyList())) {
//                try {
//                    String accession = sampleResource.getContent().getAccession();
//                    log.debug(String.format("got %s", accession));
//                    count++;
//                    boolean canary = (count % 1000 == 0);
//                    {
//                        if (canary) {
//                            long endTime = System.currentTimeMillis();
//                            long duration = (endTime - startTime);
//                            log.info("PROCESSED: samples:" + count + " rate: " + count / ((duration / 1000) + 1) + " samples per second");
//                        }
//                    }
//                } catch (Exception e) {
//                    log.error("failed after: " + count);
//                    log.error("Error getting individual sample resource: " + sampleResource.toString(), e);
//                    writer.write(e.getMessage());
//                }
//            }
//        } catch (Exception e) {
//            log.error("Error getting samples resources", e);
//        }
    }
}
