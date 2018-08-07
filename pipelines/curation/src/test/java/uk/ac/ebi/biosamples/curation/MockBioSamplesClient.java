package uk.ac.ebi.biosamples.curation;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.Resource;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.service.AapClientService;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.service.SampleValidator;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.*;

import static org.mockito.Mockito.mock;

public class MockBioSamplesClient extends BioSamplesClient {

    private Logger log = LoggerFactory.getLogger(getClass());

    private Map<String, List<Curation>> curations = new HashMap<>();

    private boolean logCurations = false;

    private PrintWriter printWriter;

    private FileWriter fileWriter;

    private int count = 0;

    public MockBioSamplesClient(URI uri, RestTemplateBuilder restTemplateBuilder,
                                SampleValidator sampleValidator, AapClientService aapClientService,
                                BioSamplesProperties bioSamplesProperties, boolean logCurations) {
        super(uri, restTemplateBuilder, sampleValidator, aapClientService, bioSamplesProperties);
        this.logCurations = logCurations;
        if (logCurations) {
            try {
                log.info("Logging curations");
                fileWriter = new FileWriter("curations.csv");
                printWriter = new PrintWriter(fileWriter);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void finalize() {
        try {
            fileWriter.close();
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Resource<CurationLink> persistCuration(String accession, Curation curation, String domain) {
        log.trace("Mocking persisting curation " + curation + " on " + accession + " in " + domain);
        if (logCurations) {
            logCuration(accession, domain, curation);
        }
        List<Curation> sampleCurations = curations.get(accession);
        if (sampleCurations == null) {
            sampleCurations = new ArrayList<>();
        }
        sampleCurations.add(curation);
        curations.put(accession, sampleCurations);
        return mock(Resource.class);
    }

    private String explainCuration(Curation curation) {
        return StringEscapeUtils
                .escapeCsv(curation.toString());
    }

    private void logCuration(String accession, String domain, Curation curation) {
        count++;
        printWriter.printf("%s,%s,%s\n", accession, domain, explainCuration(curation));
        if (count % 500 == 0) {
            log.info("Recorded " + count + " curations");
        }
    }

    public Collection<Curation> getCurations(String accession) {
        return curations.get(accession);
    }

    public void clearCurations(String accession) {
        curations.put(accession, new ArrayList<>());
    }
}



