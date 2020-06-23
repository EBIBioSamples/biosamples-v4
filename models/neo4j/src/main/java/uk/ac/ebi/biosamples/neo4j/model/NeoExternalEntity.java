package uk.ac.ebi.biosamples.neo4j.model;

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.service.ExternalReferenceUtils;

public class NeoExternalEntity {
    private String url;
    private String archive;
    private String ref;

    private NeoExternalEntity(String url, String archive, String ref) {
        this.url = url;
        this.archive = archive;
        this.ref = ref;
    }

    public String getUrl() {
        return url;
    }

    public String getArchive() {
        return archive;
    }

    public String getRef() {
        return ref;
    }

    public static NeoExternalEntity build(ExternalReference reference) {
        String externalRef = ExternalReferenceUtils.getNickname(reference).toLowerCase();
        return new NeoExternalEntity(reference.getUrl(),
                externalRef.startsWith("ega") ? "ega" : externalRef,
                ExternalReferenceUtils.getDataId(reference).orElse(String.valueOf(Math.abs(reference.getUrl().hashCode()))));
    }
}
