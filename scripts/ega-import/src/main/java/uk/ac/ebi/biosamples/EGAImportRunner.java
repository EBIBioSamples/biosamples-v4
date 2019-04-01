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


        final String dataFolderUrl = "/home/isuru/BioSamples/EGA_Import/";
        final String datasetDuoUrl = dataFolderUrl + "datasets_duo.csv";
        final String sampleDataUrl = dataFolderUrl + "sanger_released_samples.csv";
        final String egaUrl = "https://www.ebi.ac.uk/ega/datasets/";
        final ObjectMapper jsonMapper = new ObjectMapper();

        Map<String, List<String>> datasetToDuoCodesMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(datasetDuoUrl))) {
            String line = br.readLine(); //ignore header
            LOG.info("Reading file: {}, headers: {}", datasetDuoUrl, line);
            while ((line = br.readLine()) != null && !line.isEmpty()) {
                String[] record = line.replaceAll("[\"\\[\\] ]", "").split(",");
                String datasetId = record[0];
                String[] duoCodes = Arrays.copyOfRange(record, 1, record.length);

                datasetToDuoCodesMap.put(datasetId, Arrays.asList(duoCodes));
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
