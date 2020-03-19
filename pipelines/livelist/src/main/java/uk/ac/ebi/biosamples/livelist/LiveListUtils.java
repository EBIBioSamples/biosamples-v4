package uk.ac.ebi.biosamples.livelist;

import uk.ac.ebi.biosamples.model.Sample;

public class LiveListUtils {
    public static String createLiveListString(Sample sample) {
        return sample.getAccession();
    }
}
