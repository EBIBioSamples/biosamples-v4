package uk.ac.ebi.biosamples.service;

import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.model.ExternalReference;

import java.util.Optional;

public class ExternalReferenceUtils {
    private ExternalReferenceUtils() {
        //private
    }

    public static final String ENA_BASE_URL_FRAGMENT = "www.ebi.ac.uk/ena";
    public static final String ARRAYEXPRESS_BASE_URL_FRAGMENT = "www.ebi.ac.uk/arrayexpress";
    public static final String HPSCREG_URL_FRAGMENT = "hpscreg.eu/";
    public static final String DBGAP_BASE_URL_FRAGMENT = "ncbi.nlm.nih.gov/projects/gap";
    public static final String EGA_DATASET_BASE_URL_FRAGMENT = "ega-archive.org/datasets";
    public static final String EGA_SAMPLE_BASE_URL_FRAGMENT = "ega-archive.org/metadata";
    public static final String EGA_STUDY_BASE_URL_FRAGMENT = "ega-archive.org/studies";
    public static final String BIOSTUDIES_BASE_URL_FRAGMENT = "ebi.ac.uk/biostudies";
    public static final String DUO_BASE_URL = "http://purl.obolibrary.org/obo/";

    public static String getNickname(ExternalReference externalReference) {
        if (externalReference.getUrl().contains(ENA_BASE_URL_FRAGMENT)) {
            return "ENA";
        } else if (externalReference.getUrl().contains(ARRAYEXPRESS_BASE_URL_FRAGMENT)) {
            return "ArrayExpress";
        } else if (externalReference.getUrl().contains(HPSCREG_URL_FRAGMENT)) {
            return "hPSCreg";
        } else if (externalReference.getUrl().contains(DBGAP_BASE_URL_FRAGMENT)) {
            return "dbGaP";
        } else if (externalReference.getUrl().contains(EGA_DATASET_BASE_URL_FRAGMENT)) {
            return "EGA Dataset";
        } else if (externalReference.getUrl().contains(EGA_SAMPLE_BASE_URL_FRAGMENT)) {
            return "EGA Sample";
        } else if (externalReference.getUrl().contains(EGA_STUDY_BASE_URL_FRAGMENT)) {
            return "EGA Study";
        } else if (externalReference.getUrl().contains(BIOSTUDIES_BASE_URL_FRAGMENT)) {
            return "BioStudies";
        } else {
            return "other";
        }
    }

    public static Optional<String> getDataId(ExternalReference externalReference) {
        String nickname = getNickname(externalReference);
        if ("ENA".equals(nickname) || "ArrayExpress".equals(nickname) || "hPSCreg".equals(nickname)
                || "EGA Dataset".equals(nickname) || "EGA Sample".equals(nickname) || "EGA Study".equals(nickname) || "BioStudies".equals(nickname)) {
            UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(externalReference.getUrl()).build();
            String lastPathSegment = uriComponents.getPathSegments().get(uriComponents.getPathSegments().size() - 1);
            return Optional.of(lastPathSegment);
        } else if ("dbGaP".equals(nickname)) {
            UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(externalReference.getUrl()).build();
            String studyId = uriComponents.getQueryParams().getFirst("study_id");
            return Optional.of(studyId);
        }
        return Optional.empty();
    }

    public static String getDuoUrl(String duoCode) {
        return DUO_BASE_URL + duoCode.replace(":", "_");
    }
}
