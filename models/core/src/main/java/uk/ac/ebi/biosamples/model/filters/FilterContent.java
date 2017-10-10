package uk.ac.ebi.biosamples.model.filters;

public interface FilterContent {

    public Object getContent();

    public void merge(FilterContent otherContent);
}
