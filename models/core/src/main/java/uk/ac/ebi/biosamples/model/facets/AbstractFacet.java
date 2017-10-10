package uk.ac.ebi.biosamples.model.facets;

public class AbstractFacet implements Comparable<AbstractFacet>{
    private final String label;
    private final FacetType type;
    private final long count;
    private final FacetContent content;

    protected AbstractFacet(String label, FacetType type, long count, FacetContent content) {
        this.label = label;
        this.type = type;
        this.count = count;
        this.content = content;
    }

    public String getLabel() {
        return label;
    }

    public FacetType getType() {
        return type;
    }

    public long getCount() {
        return count;
    }

    public Object getContent() {
        return content.getContent();
    }

    @Override
    public int compareTo(AbstractFacet o) {
        return Long.compare(this.getCount(), o.getCount());
    }


}
