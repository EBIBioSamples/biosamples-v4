package uk.ac.ebi.biosamples.neo4j.model;

import java.util.List;

public class GraphSearchQuery {
    private List<GraphNode> nodes;
    private List<GraphLink> links;
    private int page;
    private int size;

    public List<GraphNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<GraphNode> nodes) {
        this.nodes = nodes;
    }

    public List<GraphLink> getLinks() {
        return links;
    }

    public void setLinks(List<GraphLink> links) {
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
}
