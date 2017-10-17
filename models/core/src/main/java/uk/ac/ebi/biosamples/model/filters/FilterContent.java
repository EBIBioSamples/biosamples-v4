package uk.ac.ebi.biosamples.model.filters;

public interface FilterContent {

    /**
     * Get the content of the filter
     * @return the content
     */
    public Object getContent();

//    /**
//     * Merge the current content with the other
//     * @param otherContent the other content
//     */
//    public void merge(FilterContent otherContent);

    /**
     * Return the serialization of the content
     * @return
     */
    public String getSerialization();
}
