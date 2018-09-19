package uk.ac.ebi.biosamples.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;
import uk.ac.ebi.biosamples.solr.service.SolrSampleService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AccessionsService {

    @Autowired
    private SolrSampleService solrSampleService;

    public List<String> getAccessions(String project, int limit) {
        List<Filter> filters = Collections.EMPTY_LIST;
        if (!project.isEmpty()) {
            Filter filter = FilterBuilder.create().onAttribute("project").withValue(project).build();
            filters = Collections.singletonList(filter);
        }
        List<String> domains = Collections.EMPTY_LIST;
        CursorArrayList<SolrSample> results = solrSampleService.fetchSolrSampleByText(null, filters, domains, "*", limit);
        List<String> accessions = new ArrayList<>();
        for (SolrSample solrSample : results) {
            accessions.add(solrSample.getAccession());
        }
        return accessions;
    }
}
