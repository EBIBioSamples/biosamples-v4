package uk.ac.ebi.biosamples.neo4j.model;

import uk.ac.ebi.biosamples.model.RelationshipType;

import java.util.Objects;

public class GraphLink implements Comparable<GraphLink> {
    private RelationshipType type;
    private String startNode;
    private String endNode;

    public RelationshipType getType() {
        return type;
    }

    public void setType(RelationshipType type) {
        this.type = type;
    }

    public String getStartNode() {
        return startNode;
    }

    public void setStartNode(String startNode) {
        this.startNode = startNode;
    }

    public String getEndNode() {
        return endNode;
    }

    public void setEndNode(String endNode) {
        this.endNode = endNode;
    }

    public String getQueryString(String relName) {
        String rel = (type == RelationshipType.ANY) ? relName : relName + ":" + type;
        return "(" + startNode + ")-[" + rel + "]->(" + endNode + ") ";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof GraphLink)) {
            return false;
        }
        GraphLink otherLink = (GraphLink) other;
        return this.type.equals(otherLink.getType()) &&
                this.startNode.equals(otherLink.getStartNode()) && this.endNode.equals(otherLink.getEndNode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, startNode, endNode);
    }

    @Override
    public int compareTo(GraphLink other) {
        if (!this.type.equals(other.getType())) {
            return this.type.compareTo(other.getType());
        }

        if (!this.startNode.equals(other.getStartNode())) {
            return this.startNode.compareTo(other.getStartNode());
        }

        return this.endNode.compareTo(other.getEndNode());
    }
}
