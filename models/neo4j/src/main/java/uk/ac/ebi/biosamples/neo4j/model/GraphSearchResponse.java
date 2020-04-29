package uk.ac.ebi.biosamples.neo4j.model;

import java.util.List;

public class GraphSearchResponse {
    private List<GraphNode> samples;
    private int page;
    private int size;

    public List<GraphNode> getSamples() {
        return samples;
    }

    public void setSamples(List<GraphNode> samples) {
        this.samples = samples;
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
