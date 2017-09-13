package uk.ac.ebi.biosamples.model.facets;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FacetsBuilder {

    private List<StringListFacet> facets = new LinkedList<>();

    public FacetsBuilder addFacet(StringListFacet facet) {

        facets.add(facet);

        //sort it into a descending order
        Collections.sort(facets);
        Collections.reverse(facets);

        return this;
    }

    public List<StringListFacet> build() {
        return Collections.unmodifiableList(new LinkedList<>(facets));
    }
}
