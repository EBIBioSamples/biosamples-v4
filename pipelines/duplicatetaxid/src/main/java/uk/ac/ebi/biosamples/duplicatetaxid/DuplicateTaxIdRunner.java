package uk.ac.ebi.biosamples.duplicatetaxid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

@Component
public class DuplicateTaxIdRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicateTaxIdRunner.class);

    private final BioSamplesClient bioSamplesClient;

    public DuplicateTaxIdRunner(BioSamplesClient bioSamplesClient) {
        //ensure the client is public
        if (bioSamplesClient.getPublicClient().isPresent()) {
            this.bioSamplesClient = bioSamplesClient.getPublicClient().get();
        } else {
            this.bioSamplesClient = bioSamplesClient;
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String duplicateTaxIdsFile = "duplicateTaxIds.txt";
        if (args.getNonOptionArgs().size() > 0) {
            duplicateTaxIdsFile = args.getNonOptionArgs().get(0);
        }
        long startTime = System.nanoTime();
        int sampleCount = 0;
        int duplicateCount = 0;
        try (
                Writer duplicateTaxIdsWriter = args.getOptionValues("gzip") == null
                        ? new OutputStreamWriter(new FileOutputStream(duplicateTaxIdsFile), "UTF-8")
                        : new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(duplicateTaxIdsFile)), "UTF-8");
        ) {
            LOGGER.info("Starting duplicate TaxIds export");
            for (Resource<Sample> sampleResource : bioSamplesClient.fetchSampleResourceAll()) {
                LOGGER.trace("Handling " + sampleResource);
                Sample sample = sampleResource.getContent();
                int numberOfTaxIds = determineNumberOfTaxIds(sample);
                if (numberOfTaxIds > 1) {
                    LOGGER.info("Found " + sample.getAccession() + ":" + numberOfTaxIds);
                    duplicateTaxIdsWriter.write(sample.getAccession() + "\t" + numberOfTaxIds);
                    duplicateTaxIdsWriter.write("\n");
                    duplicateCount++;
                }
                if (sampleCount % 10000 == 0) {
                    LOGGER.info("Running duplicate TaxIds found: exported " + duplicateCount + " duplicates out of " + sampleCount + " samples in " + ((System.nanoTime() - startTime) / 1000000000l) + "s");
                }
                sampleCount++;
            }
        }
        long elapsed = System.nanoTime() - startTime;
        LOGGER.info("Completed duplicate TaxIds export:  " + sampleCount + " samples exported in " + (elapsed / 1000000000l) + "s");
    }

    private int determineNumberOfTaxIds(Sample sample) {
        List<Integer> taxIds = new ArrayList<>();
        for (Attribute attribute : sample.getAttributes()) {
            if (attribute.getType().toLowerCase().equalsIgnoreCase("Organism") && !attribute.getIri().isEmpty()) {
                attribute.getIri().stream().
                        map(Object::toString).
                        map(this::extractTaxIdFromIri).
                        forEach(taxIds::add);
            }
        }
        return taxIds.size();
    }


    private int extractTaxIdFromIri(String iri) {
        if (iri.isEmpty()) return 0;
        String segments[] = iri.split("NCBITaxon_");
        try {
            return Integer.parseInt(segments[segments.length - 1]);
        } catch (NumberFormatException e) {
            return 0;
        }

    }

}
