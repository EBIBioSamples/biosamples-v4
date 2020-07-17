package uk.ac.ebi.biosamples.service.certification;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.certification.CurationResult;
import uk.ac.ebi.biosamples.model.certification.HasCuratedSample;
import uk.ac.ebi.biosamples.model.certification.SampleDocument;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class Applicator {
    private static Logger LOG = LoggerFactory.getLogger(Applicator.class);

    public SampleDocument apply(HasCuratedSample curationApplicable) {
        if (curationApplicable == null) {
            String message = "cannot apply a null curation applyable";
            LOG.warn(message);
            throw new IllegalArgumentException(message);
        }

        SampleDocument sampleDocument = curationApplicable.getSampleDocument();
        String document = makePretty(sampleDocument.getDocument());
        String updatedDocument = document;

        for (CurationResult curationResult : curationApplicable.getCurationResults()) {
            String pattern = String.format("\\\"%s\\\"\\s?[:]\\s?\\[\\W+?text\\\"\\s?[:]\\s?\\s\\\"(%s)\\\"", curationResult.getCharacteristic(), curationResult.getBefore());
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(updatedDocument);
            if (m.find()) {
                updatedDocument = updatedDocument.replace(m.group(1), curationResult.getAfter());
            } else {
                LOG.warn(String.format("%s failed to apply %s to sampleDocument", sampleDocument.getAccession(), curationResult.getCharacteristic()));
            }
        }

        SampleDocument curatedSampleDocument = new SampleDocument(sampleDocument.getAccession(), updatedDocument);

        return curatedSampleDocument;
    }

    private String makePretty(String document) {
        JSONObject json = new JSONObject(document);
        return json.toString(2);
    }
}
