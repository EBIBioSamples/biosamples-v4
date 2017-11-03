package uk.ac.ebi.biosamples.solr.model.field;

import org.springframework.data.solr.core.query.Criteria;
import uk.ac.ebi.biosamples.model.filter.Filter;

public interface FilterCriteriaBuilder {

    Criteria getFilterCriteria(Filter filter);
}
