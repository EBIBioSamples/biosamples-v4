package uk.ac.ebi.biosamples.legacy.json.domain;

import org.springframework.hateoas.core.Relation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

//@JsonDeserialize(using = SampleRelationsDeserializer.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
@Relation(value = "externallinkrelations", collectionRelation = "externallinksrelations")
public class ExternalLinksRelation { //FIXME ExternalLink relations should be mapped as entities in v4. How do we handle this?
    private String url;

    public ExternalLinksRelation(String url) {
        this.url = url;
    }


    @JsonProperty
    public String url() {
        return this.url;
    }
}
