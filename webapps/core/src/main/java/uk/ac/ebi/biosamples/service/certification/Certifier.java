package uk.ac.ebi.biosamples.service.certification;

import org.everit.json.schema.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.certification.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
public class Certifier {
    private static Logger LOG = LoggerFactory.getLogger(Certifier.class);
    private static Logger EVENTS = LoggerFactory.getLogger("events");

    private ConfigLoader configLoader;
    private Validator validator;
    private Applicator applicator;

    public Certifier(ConfigLoader configLoader, Validator validator, Applicator applicator) {
        this.validator = validator;
        this.configLoader = configLoader;
        this.applicator = applicator;
    }

    public CertificationResult certify(SampleDocument sampleDocument) {
        if (sampleDocument == null) {
            String message = "cannot certify a null sampleDocument";
            LOG.warn(message);
            throw new IllegalArgumentException(message);
        }
        return certify(sampleDocument, Collections.EMPTY_LIST);
    }

    private CertificationResult certify(SampleDocument sampleDocument, List<CurationResult> curationResults) {
        CertificationResult certificationResult = new CertificationResult(sampleDocument.getAccession());
        boolean certified = false;
        for (Checklist checklist : configLoader.config.getChecklists()) {
            try {
                validator.validate(checklist.getFileName(), sampleDocument.getDocument());
                EVENTS.info(String.format("%s validation successful against %s", sampleDocument.getAccession(), checklist.getID()));
                certified = true;
                certificationResult.add(new Certificate(sampleDocument, curationResults, checklist));
                EVENTS.info(String.format("%s issued certificate %s", sampleDocument.getAccession(), checklist.getID()));
            } catch (IOException ioe) {
                LOG.error(String.format("cannot open schema at %s", checklist.getFileName()), ioe);
            } catch (ValidationException ve) {
                EVENTS.info(String.format("%s validation failed against %s", sampleDocument.getAccession(), checklist.getID()));
            }
        }
        if (!certified) {
            EVENTS.info(String.format("%s not certified", sampleDocument.getAccession()));
        }
        return certificationResult;
    }

    public CertificationResult certify(HasCuratedSample hasCuratedSample) {
        if (hasCuratedSample == null) {
            String message = "cannot certify a null plan result";
            LOG.warn(message);
            throw new IllegalArgumentException(message);
        }
        return certify(applicator.apply(hasCuratedSample), hasCuratedSample.getCurationResults());
    }
}
