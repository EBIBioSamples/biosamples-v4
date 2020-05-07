package uk.ac.ebi.biosamples.neo4j.model;

public class NeoExternalRelationship {
    private String url;
    private String archive;
    private String ref;

    private NeoExternalRelationship(String url, String archive, String ref) {
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

    public static NeoExternalRelationship build(String url, String archive, String ref) {
        return new NeoExternalRelationship(url, archive, ref);
    }
}
