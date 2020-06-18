package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.AccessionFtpUrlPair;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.amr.AMREntry;
import uk.ac.ebi.biosamples.model.structured.amr.AMRTable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AmrDataLoaderService {
    private final static Logger log = LoggerFactory.getLogger(AmrDataLoaderService.class);
    private static final String BSD_SAMPLE_PREFIX = "SA";
    private static final String FTP = "ftp";
    private static final String MD_5 = "md5";
    private static final String HTTP = "http://";
    private static final String antibiogram = "\"AMR_ANTIBIOGRAM\"";
    private static final String URL = "https://www.ebi.ac.uk/ena/portal/api/search?result=analysis&query=analysis_type=" + antibiogram + "&dataPortal=pathogen&dccDataOnly=false&fields=analysis_accession,country,region,scientific_name,location,sample_accession,tax_id,submitted_ftp,first_public,last_updated&sortFields=scientific_name,country&limit=0";
    public static final String TAB = "\t";

    public Map<String, Set<AbstractData>> loadAmrData() {
        log.info("Loading ENA-AMR data");

        Map<String, Set<AbstractData>> sampleToAmrMap = new HashMap<>();
        List<AccessionFtpUrlPair> pairList;

        try {
            pairList = requestHttpAndGetAccessionFtpUrlPairs();

            if (pairList.size() == 0) {
                log.info("Unable to fetch ENA-AMR Antibiogram data from ENA API, Timed out waiting for connection");
            } else {
                downloadFtpContent(pairList, sampleToAmrMap);
            }
        } catch (Exception e) {
            log.info("An exception occured while processing AMR data " + e.getMessage());
        }

        return sampleToAmrMap;
    }

    private static List<AccessionFtpUrlPair> requestHttpAndGetAccessionFtpUrlPairs() throws Exception {
        final URL enaApiUrl = new URL(AmrDataLoaderService.URL);
        final HttpURLConnection conn = (HttpURLConnection) enaApiUrl.openConnection();
        List<AccessionFtpUrlPair> pairList = new ArrayList<>();

        try {
            if (getResponseFromEnaApi(conn) == 200) {
                pairList = doGetAccessionFtpUrlPairs(enaApiUrl);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            conn.disconnect();
        }

        return pairList;
    }

    private static int getResponseFromEnaApi(final HttpURLConnection conn) throws IOException {
        int response;

        conn.setRequestMethod("GET");
        conn.connect();
        response = conn.getResponseCode();

        return response;
    }

    private static List<AccessionFtpUrlPair> doGetAccessionFtpUrlPairs(final URL url) {
        final List<AccessionFtpUrlPair> accessionFtpUrlPairs = new ArrayList<>();

        try {
            BufferedReader bufferedReader = getReader(url);

            bufferedReader.lines().forEach(line -> accessionFtpUrlPairs.add(getAccessionFtpUrlPair(line)));
        } catch (final IOException e) {
            log.info("Failed to get and parse accession and FTP pairs for URL " + url.toString());
        }

        return accessionFtpUrlPairs;
    }

    private static BufferedReader getReader(final URL url) throws IOException {
        return new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
    }

    private static AccessionFtpUrlPair getAccessionFtpUrlPair(final String line) {
        final StringTokenizer tokenizer = new StringTokenizer(line, TAB);
        final AccessionFtpUrlPair accessionFtpUrlPair = new AccessionFtpUrlPair();

        while (tokenizer.hasMoreTokens()) {
            final String value = tokenizer.nextToken();

            if (value.startsWith(BSD_SAMPLE_PREFIX)) {
                accessionFtpUrlPair.setAccession(value);
            }

            if (value.startsWith(FTP)) {
                dealWithSemicolon(value, accessionFtpUrlPair);
            }
        }

        return accessionFtpUrlPair;
    }

    private static void dealWithSemicolon(final String value, final AccessionFtpUrlPair accessionFtpUrlPair) {
        final int index = value.indexOf(';');
        final String option1 = value.substring(index + 1);
        final String option2 = value.substring(0, index);

        if (!option1.endsWith(MD_5)) {
            accessionFtpUrlPair.setFtpUrl(HTTP + option1);
        } else {
            accessionFtpUrlPair.setFtpUrl(HTTP + option2);
        }
    }

    private Map<String, Set<AbstractData>> downloadFtpContent(final List<AccessionFtpUrlPair> pairList, Map<String, Set<AbstractData>> sampleToAmrMap) {
        pairList.forEach(pair -> {
            try {
                String accession = pair.getAccession();

                if (accession != null)
                    sampleToAmrMap.put(accession, fetchSampleAndProcessAmrData(new URL(pair.getFtpUrl()), accession));
            } catch (MalformedURLException e) {
                log.info("FTP URL not correctly formed " + pair.getFtpUrl());
            }
        });

        return sampleToAmrMap;
    }

    private Set<AbstractData> fetchSampleAndProcessAmrData(final URL url, final String accession) {
        Set<AbstractData> amrData = new HashSet<>();

        try {
            amrData = processAmrData(processAmrLines(getReader(url)), accession);
        } catch (final IOException ioe) {
            ioe.printStackTrace();
            log.info("A IO Exception occurrence detected");

            if (amrData.size() == 0)
                log.info("Couldn't process AMR data for " + accession);
        }

        return amrData;
    }

    private List<String> processAmrLines(BufferedReader bufferedReader) {
        return bufferedReader.lines().skip(1).map(this::removeBioSampleId).map(this::dealWithExtraTabs).collect(Collectors.toList());
    }

    private Set<AbstractData> processAmrData(List<String> lines, String accession) {
        final Set<AbstractData> structuredData = new HashSet<>();
        final AMRTable.Builder amrTableBuilder = new AMRTable.Builder("http://localhost:8081/biosamples/schemas/amr.json", "self.BiosampleImportENA");

        lines.forEach(line -> {
            final CsvMapper mapper = new CsvMapper();
            final CsvSchema schema = mapper.schemaFor(AMREntry.class).withColumnSeparator('\t');
            final ObjectReader r = mapper.readerFor(AMREntry.class).with(schema);

            try {
                final AMREntry amrEntry = r.readValue(line);

                amrTableBuilder.addEntry(amrEntry);
            } catch (final Exception e) {
                log.error("Error in parsing AMR data for sample " + accession);
            }
        });

        structuredData.add(amrTableBuilder.build());

        return structuredData;
    }

    private String removeBioSampleId(String line) {
        return line.substring(line.indexOf(AmrDataLoaderService.TAB) + 1);
    }

    private String dealWithExtraTabs(String line) {
        while (line.endsWith(AmrDataLoaderService.TAB)) {
            line = line.substring(0, line.length() - 1);
        }

        return line;
    }
}
