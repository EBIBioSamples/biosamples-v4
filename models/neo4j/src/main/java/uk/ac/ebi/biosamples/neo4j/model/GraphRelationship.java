package uk.ac.ebi.biosamples.neo4j.model;

import uk.ac.ebi.biosamples.model.RelationshipType;

public class GraphRelationship {
    private RelationshipType type;
    private GraphNode startNode;
    private GraphNode endNode;

    public RelationshipType getType() {
        return type;
    }

    public void setType(RelationshipType type) {
        this.type = type;
    }

    public GraphNode getStartNode() {
        return startNode;
    }

    public void setStartNode(GraphNode startNode) {
        this.startNode = startNode;
    }

    public GraphNode getEndNode() {
        return endNode;
    }

    public void setEndNode(GraphNode endNode) {
        this.endNode = endNode;
    }

    public String getQueryString() {
        return "(a" + startNode.getQueryString() + ")-[r:" + type + "]-(b" + endNode.getQueryString() + ")";
    }
}
