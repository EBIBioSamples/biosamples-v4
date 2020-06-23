package uk.ac.ebi.biosamples.neo4j.model;

import java.util.List;
import java.util.Map;

public class CypherQuery {
    private String query;
    private List<Map<String, Object>> response;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<Map<String, Object>> getResponse() {
        return response;
    }

    public void setResponse(List<Map<String, Object>> response) {
        this.response = response;
    }
}
