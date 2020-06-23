package uk.ac.ebi.biosamples.neo4j.model;

import java.util.Set;

public class GraphSearchQuery {
    private Set<GraphNode> nodes;
    private Set<GraphLink> links;
    private int page;
    private int size;

    public Set<GraphNode> getNodes() {
        return nodes;
    }

    public void setNodes(Set<GraphNode> nodes) {
        this.nodes = nodes;
    }

    public Set<GraphLink> getLinks() {
        return links;
    }

    public void setLinks(Set<GraphLink> links) {
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
