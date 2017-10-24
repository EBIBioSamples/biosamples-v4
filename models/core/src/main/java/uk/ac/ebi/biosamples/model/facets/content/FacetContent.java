package uk.ac.ebi.biosamples.model.facets.content;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = FacetContentDeserializer.class)
public interface FacetContent {
}
