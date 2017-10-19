package uk.ac.ebi.biosamples.model.filters;

public interface FilterBuilder {

    public Filter build();

    public FilterBuilder parseValue(String filterSerialized);

}
