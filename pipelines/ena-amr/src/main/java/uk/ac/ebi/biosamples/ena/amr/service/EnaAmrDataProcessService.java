package uk.ac.ebi.biosamples.ena.amr.service;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ena.amr.AmrRunner;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.amr.AMREntry;
import uk.ac.ebi.biosamples.model.structured.amr.AMRTable;

import java.io.BufferedReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Service
public class EnaAmrDataProcessService {
    private final static Logger log = LoggerFactory.getLogger(EnaAmrDataProcessService.class);
    public final static ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<String>();

    public void processAmrData(final List<String> lines, final Sample sample, final BioSamplesClient client) {
        /*String[] dilutionMethods = new String[]{"Broth dilution", "Microbroth dilution", "Agar dilution"};
        String[] diffusionMethods = new String[]{"Disc-diffusion", "Neo-sensitabs", "Etest"};*/

        final String accession = sample.getAccession();

        if (!accession.startsWith(AmrRunner.SAMEA) && sample.getData().size() > 0)
            log.info("Not an ENA sample and AMR data already present in sample for " + accession);
        else
            processAmrData(lines, sample, client, accession);
    }

    public List<String> processAmrLines(BufferedReader bufferedReader) {
        return bufferedReader.lines().skip(1).map(this::removeBioSampleId).map(this::dealWithExtraTabs).collect(Collectors.toList());
    }

    private void processAmrData(List<String> lines, Sample sample, BioSamplesClient client, String accession) {
        final Set<AbstractData> structuredData = new HashSet<>();
        final AMRTable.Builder amrTableBuilder = new AMRTable.Builder("http://localhost:8081/biosamples/schemas/amr.json", "self.BiosampleImportENA");

        lines.forEach(line -> {
            final CsvMapper mapper = new CsvMapper();
            final CsvSchema schema = mapper.schemaFor(AMREntry.class).withColumnSeparator('\t');
            final ObjectReader r = mapper.readerFor(AMREntry.class).with(schema);

            try {
                AMREntry amrEntry = r.readValue(line);
                amrTableBuilder.addEntry(amrEntry);
            } catch (final Exception e) {
                log.error("Error in parsing AMR data for sample " + accession);
                failedQueue.add(accession);
            }
        });

        structuredData.add(amrTableBuilder.build());

        if (structuredData.size() > 0) {
            final Sample sampleNew = Sample.Builder.fromSample(sample).withData(structuredData).build();
            client.persistSampleResource(sampleNew);

            log.info("Submitted sample " + accession + " with structured data");
        }
    }

    private String removeBioSampleId(final String line) {
        return line.substring(line.indexOf(AmrRunner.TAB) + 1);
    }

    private String dealWithExtraTabs(String line) {
        while (line.endsWith(AmrRunner.TAB)) {
            line = line.substring(0, line.length() - 1);
        }

        return line;
    }
}
