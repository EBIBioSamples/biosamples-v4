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
//        String accession = "SAMEA5648975";
//        String jwt = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2V4cGxvcmUuYWFpLmViaS5hYy51ay9zcCIsImp0aSI6IkUxSFE3SDFrME1ldjR3SGFnSUtvT0EiLCJpYXQiOjE1NTM2MTc3NDksInN1YiI6InVzci04NjYyYmEwNS0xMzRhLTRiMTQtODViMC04ZTUyY2I5ZmVlOGQiLCJlbWFpbCI6ImlzdXJ1QGViaS5hYy51ayIsIm5pY2tuYW1lIjoiaXN1cnVsIiwibmFtZSI6IklzdXJ1IExpeWFuYWdlIiwiZG9tYWlucyI6WyJzZWxmLklzdXJ1MSIsInN1YnMudGVzdC10ZWFtLTIzIiwic3Vicy5kZXYtdGVhbS0xNDAiXSwiZXhwIjoxNTUzNjIxMzQ5fQ.gOkxhArFcACn77_qieX0VA9gNsVyYjqK0VRN3ygGOEwN6_roPyajyPuLmePqC14FgzXcQDEwyYNmKN0LG8oUB7xncNifqblBmYQ29rneK3_e5nUnZEPKoHNkJeLo4VORS1TUVUhCm3VrILrbbX-wL5W3gh3vwH9nCUieL1LSNHjebeXomEz0unLl573iEdrVi-F7SpVkRFYiPZ0kCaoyEJ1YsYGP9pp18GmOfmWtxBXCPaEh9zvpY1_HL-F7TOV9Fb93EYcoVpRuZYpZH4pV-CRTNmOc6mz-yjhc1EOP9KmlS6sIryJhVG10IS-nnZWjj3-MdQBmlNseo1wVfWAdJw";
//        Optional<Resource<Sample>> sampleResource = bioSamplesClient.fetchSampleResource(accession, jwt);
//        if (sampleResource.isPresent()) {
//            System.out.println(sampleResource.get().getContent());
//        } else {
//            System.out.println("Cant find the sample");
//        }


        String dataFolderUrl = "/home/isuru/BioSamples/EGA_Import/";
        String datasetDuoUrl = dataFolderUrl + "datasets_duo.csv";
        String sampleDataUrl = dataFolderUrl + "sanger_released_samples.csv";
        String egaUrl = "https://www.ebi.ac.uk/ega/datasets/";

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
            String line = br.readLine(); //ignore header
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
                            .addAttribute(Attribute.build("ega dataset id", datasetId))
                            .addAttribute(Attribute.build("ega sample id", egaId))
                            .addExternalReference(ExternalReference.build(egaUrl + datasetId))
                            .addAllDuoCodes(duoCodes)
                            .build();

//                    bioSamplesClient.persistSampleResource(updatedSample);
                } else {
                    LOG.warn("Sample not present in biosamples: {}", accession);
                }
            }

        } catch (IOException e) {
            LOG.error("Couldn't read file: {}", datasetDuoUrl);
        }


        String accession = "SAMEA2062883";
        Optional<Resource<Sample>> sampleResource = bioSamplesClient.fetchSampleResource(accession);
        if (sampleResource.isPresent()) {
            System.out.println(sampleResource.get().getContent());
        } else {
            System.out.println("Cant find the sample");
        }


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
