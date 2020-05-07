package uk.ac.ebi.biosamples.neo4j.model;

import java.util.List;

public class GraphSearchRequest {
    private List<GraphRelationship> relationships;
    private int page;
    private int size;

    public List<GraphRelationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<GraphRelationship> relationships) {
        this.relationships = relationships;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
