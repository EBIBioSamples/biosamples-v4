package uk.ac.ebi.biosamples.neo4j.model;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class GraphNode implements Comparable<GraphNode> {
    private String id;
    private String type;
    private Map<String, String> attributes;

    public GraphNode() {
        //default constructor
    }

    public GraphNode(Map<String, Object> nodeAsMap) {
        type = String.valueOf(nodeAsMap.get("type"));
        attributes = (Map<String, String>) nodeAsMap.get("attributes");
        id = attributes.get("accession");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getQueryString() {
        String queryString;
        if (attributes != null) {
            StringJoiner joiner = new StringJoiner(",");
            for (Map.Entry<String, String> e : attributes.entrySet()) {
                joiner.add(e.getKey().replaceAll("\\s+", "").toLowerCase() + ":'" + e.getValue().toLowerCase() + "'");
            }
            queryString = ":" + type + "{" + joiner.toString() + "}";
        } else {
            queryString = ":" + type;
        }
        return queryString;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof GraphNode && this.type.equalsIgnoreCase(((GraphNode) other).getType())) {
            return this.id.equals(((GraphNode) other).id);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type.toUpperCase(), id);
    }

    @Override
    public int compareTo(GraphNode other) {
        if (this.type.equalsIgnoreCase(other.getType())) {
            return this.id.compareTo(other.getId());
        } else {
            return this.type.compareTo(other.getType());
        }
    }
}
