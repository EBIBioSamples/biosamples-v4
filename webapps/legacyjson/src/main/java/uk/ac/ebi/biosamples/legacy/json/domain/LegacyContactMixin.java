package uk.ac.ebi.biosamples.legacy.json.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface LegacyContactMixin {

    @JsonIgnore
    abstract String getFirstName();

    @JsonIgnore
    abstract String getLastName();
    @JsonIgnore
    abstract String getMidInitials();

    @JsonIgnore
    abstract String getAffiliation();

    @JsonIgnore
    abstract String getURL();

    @JsonIgnore
    abstract String getRole();

    @JsonIgnore
    abstract String getEmail();

}
