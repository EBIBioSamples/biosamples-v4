package uk.ac.ebi.biosamples.model.facets;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FacetsBuilder {

    private List<Facet> facets = new LinkedList<>();

    public FacetsBuilder addFacet(Facet facet) {

        facets.add(facet);

        //sort it into a descending order
        Collections.sort(facets);
        Collections.reverse(facets);

        return this;
    }

    public List<Facet> build() {
        return Collections.unmodifiableList(new LinkedList<>(facets));
    }
}
