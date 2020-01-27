package uk.ac.ebi.biosamples.ena.amr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ena.amr.service.EnaAmrDataProcessService;
import uk.ac.ebi.biosamples.model.Sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

@Component
public class AmrRunner implements ApplicationRunner {
    @Autowired
    EnaAmrDataProcessService enaAmrDataProcessService;
    @Autowired
    BioSamplesClient bioSamplesClient;

    private static final String antibiogram = "\"AMR_ANTIBIOGRAM\"";
    private static final String URL = "https://www.ebi.ac.uk/ena/portal/api/search?result=analysis&query=analysis_type=" + antibiogram + "&dataPortal=pathogen&dccDataOnly=false&fields=analysis_accession,country,region,scientific_name,location,sample_accession,tax_id,submitted_ftp,first_public,last_updated&sortFields=scientific_name,country&limit=0";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<AccessionFtpUrlPair> pairList = requestHttpAndGetAccessionFtpUrlPairs(new URL(URL));
        downloadFtpContent(pairList);
    }

    private static List<AccessionFtpUrlPair> requestHttpAndGetAccessionFtpUrlPairs(final URL url) throws Exception {
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        List<AccessionFtpUrlPair> pairList = new ArrayList<>();
        int response;

        try {
            if (getResponseFromEnaApi(conn) == 200) {
                pairList = doHttpCall(url);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            conn.disconnect();
        }

        return pairList;
    }

    private static int getResponseFromEnaApi(HttpURLConnection conn) throws IOException {
        int response;

        conn.setRequestMethod("GET");
        conn.connect();
        response = conn.getResponseCode();

        return response;
    }

    private void doHttpCall(final URL url, String accession) {
        try {
            BufferedReader bufferedReader = getReader(url);
            Optional<Resource<Sample>> sample = bioSamplesClient.fetchSampleResource(accession, Optional.of(new ArrayList<String>()));

            if (sample.isPresent()) {
                List<String> amrLines = enaAmrDataProcessService.processAmrLines(bufferedReader);
                System.out.println(amrLines.size());
                enaAmrDataProcessService.processAmrRows(bufferedReader, accession, sample.get().getContent(), bioSamplesClient);
            } else {
                System.out.println(accession + " doesn't exist");
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    private static List<AccessionFtpUrlPair> doHttpCall(final URL url) {
        final List<AccessionFtpUrlPair> accessionFtpUrlPairs = new ArrayList<>();

        try {
            BufferedReader bufferedReader = getReader(url);

            bufferedReader.lines().forEach(line -> accessionFtpUrlPairs.add(getAccessionFtpUrlPair(line)));
        } catch (IOException e) {
            // e.printStackTrace();
        }

        return accessionFtpUrlPairs;
    }

    private static void processAmrRows(String line, String accession) {
        System.out.println(line);
    }

    private static BufferedReader getReader(URL url) throws IOException {
        URLConnection urlc = url.openConnection();
        InputStream is = urlc.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(is);

        return new BufferedReader(inputStreamReader);
    }

    private static AccessionFtpUrlPair getAccessionFtpUrlPair(String line) {
        final StringTokenizer tokenizer = new StringTokenizer(line, "\t");
        final AccessionFtpUrlPair accessionFtpUrlPair = new AccessionFtpUrlPair();

        while (tokenizer.hasMoreTokens()) {
            String value = tokenizer.nextToken();
            System.out.println(value);

            if (value.startsWith("SA")) {
                accessionFtpUrlPair.setAccession(value);
            }

            if (value.startsWith("ftp")) {
                dealWithSemicolon(value, accessionFtpUrlPair);
            }
        }

        return accessionFtpUrlPair;
    }

    private static void dealWithSemicolon(String value, AccessionFtpUrlPair accessionFtpUrlPair) {
        int index = value.indexOf(';');
        String option1 = value.substring(index, value.length());
        String option2 = value.substring(0, index);

        if (!option1.endsWith("md5")) {
            accessionFtpUrlPair.setFtpUrl("http://" + option1);
        } else {
            accessionFtpUrlPair.setFtpUrl("http://" + option2);
        }
    }

    private void downloadFtpContent(List<AccessionFtpUrlPair> pairList) {
        pairList.forEach(pair -> {
            try {
                doHttpCall(new URL(pair.getFtpUrl()), pair.getAccession());
            } catch (MalformedURLException e) {
                //e.printStackTrace();
            }
        });
    }
}
