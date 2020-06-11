package uk.ac.ebi.biosamples.neo4j.model;

import java.util.List;

public class GraphSearchResponse {
    private List<GraphNode> nodes;
    private List<GraphRelationship> links;
    private int page;
    private int size;
    private int total;

    public List<GraphNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<GraphNode> nodes) {
        this.nodes = nodes;
    }

    public List<GraphRelationship> getLinks() {
        return links;
    }

    public void setLinks(List<GraphRelationship> links) {
        this.links = links;
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

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
