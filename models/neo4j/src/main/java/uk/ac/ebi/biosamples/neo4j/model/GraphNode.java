package uk.ac.ebi.biosamples.neo4j.model;

import org.neo4j.driver.Value;

import java.util.Map;
import java.util.StringJoiner;

public class GraphNode {
    private String id;
    private String type;
    private Map<String, String> attributes;

    public GraphNode() {
        //default constructor
    }

    public GraphNode(Map<String, Object> nodeAsMap) {
        type = String.valueOf(nodeAsMap.get("type"));
        attributes = (Map<String, String>)nodeAsMap.get("attributes");
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
                joiner.add(e.getKey() + ":'" + e.getValue() + "'");
            }
            queryString = ":" + type + "{" + joiner.toString() + "}";
        } else {
            queryString = ":" + type;
        }
        return  queryString;
    }
}
