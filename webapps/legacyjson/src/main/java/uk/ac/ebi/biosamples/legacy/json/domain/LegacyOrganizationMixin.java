package uk.ac.ebi.biosamples.legacy.json.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface LegacyOrganizationMixin {
    @JsonIgnore
    abstract String getAddress();

    @JsonProperty("URL")
    abstract String getUrl();
}
