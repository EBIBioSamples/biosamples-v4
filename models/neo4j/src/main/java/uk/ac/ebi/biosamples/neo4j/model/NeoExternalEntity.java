package uk.ac.ebi.biosamples.neo4j.model;

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

    public static NeoExternalEntity build(String url, String archive, String ref) {
        return new NeoExternalEntity(url, archive, ref);
    }
}
