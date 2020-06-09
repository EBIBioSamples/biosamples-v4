package uk.ac.ebi.biosamples.neo4j.model;

import uk.ac.ebi.biosamples.model.RelationshipType;

public class GraphLink {
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
        return "(" + startNode + ")-[" + relName + ":" + type + "]->(" + endNode + ") ";
    }
}
