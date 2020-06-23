package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.neo4j.model.GraphSearchQuery;
import uk.ac.ebi.biosamples.neo4j.repo.NeoSampleRepository;

import java.util.List;
import java.util.Map;

@Service
public class GraphSearchService {
    private static final Logger LOG = LoggerFactory.getLogger(GraphSearchService.class);

    private NeoSampleRepository neoSampleRepository;
    private SampleService sampleService;

    public GraphSearchService(NeoSampleRepository neoSampleRepository, SampleService sampleService) {
        this.neoSampleRepository = neoSampleRepository;
        this.sampleService = sampleService;
    }

    public List<Map<String, Object>> executeCypher(String query) {
        return neoSampleRepository.executeCypher(query);
    }

    public GraphSearchQuery graphSearch(GraphSearchQuery searchQuery, int limit, int skip) {
        //todo add total samples...
        return neoSampleRepository.graphSearch(searchQuery, limit, skip);
    }
}
