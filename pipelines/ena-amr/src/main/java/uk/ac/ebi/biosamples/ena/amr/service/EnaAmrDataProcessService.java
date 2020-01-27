package uk.ac.ebi.biosamples.ena.amr.service;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.amr.AMREntry;
import uk.ac.ebi.biosamples.model.structured.amr.AMRTable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EnaAmrDataProcessService {
    public void processAmrRows(BufferedReader reader, String accession, Sample sample, BioSamplesClient client) {
        Set<AbstractData> structuredData = new HashSet<>();
        AMRTable.Builder amrTableBuilder = new AMRTable.Builder("test");
        List<AMREntry> amrEntryList = new ArrayList<>();
        String[] dilutionMethods = new String[]{"Broth dilution", "Microbroth dilution", "Agar dilution"};
        String[] diffusionMethods = new String[]{"Disc-diffusion", "Neo-sensitabs", "Etest"};

        List<AMREntry> listOfEntries = new ArrayList<>();

        reader.lines().skip(1).forEach(line -> {
            //line = removeBioSampleId(line);
            CsvMapper mapper = new CsvMapper();
            CsvSchema schema = mapper.schemaFor(AMREntry.class).withColumnSeparator('\t');
            ObjectReader r = mapper.readerFor(AMREntry.class).with(schema);

            try {
                System.out.println(line);
                AMREntry amrEntry = r.readValue(line);
                amrTableBuilder.addEntry(amrEntry);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        structuredData.add(amrTableBuilder.build());
        Sample sampleNew = Sample.Builder.fromSample(sample).withData(structuredData).build();
        client.persistSampleResource(sampleNew);
    }

    private String removeBioSampleId(String line) {
        return line.substring(line.indexOf('\t'), line.length());
    }

    public List<String> processAmrLines(BufferedReader bufferedReader) {
        return bufferedReader.lines().skip(1).map(line -> removeBioSampleId(line)).collect(Collectors.toList());
    }
}
