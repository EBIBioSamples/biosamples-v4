package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.RelationshipType;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo4j.model.GraphNode;
import uk.ac.ebi.biosamples.neo4j.model.GraphSearchQuery;
import uk.ac.ebi.biosamples.neo4j.model.GraphSearchRequest;
import uk.ac.ebi.biosamples.neo4j.model.NeoSample;
import uk.ac.ebi.biosamples.neo4j.repo.NeoSampleRepository;

import java.util.*;

import static uk.ac.ebi.biosamples.model.StaticViewWrapper.SAMPLE_CURATED_REPO;

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

    public List<Map<String, Object>> fetchByRelationship(GraphSearchRequest request) {
        int effectiveLimit = request.getSize() == 0 ? 10 : request.getSize();
        List<Map<String, Object>> sampleList =
                neoSampleRepository.getByRelationship(request.getRelationships(), request.getPage(), effectiveLimit);
        List<Sample> samples = new ArrayList<>(sampleList.size());

        /*for (NeoSample neoSample : neoSamples) {
            Optional<Sample> sample = sampleService.fetch(neoSample.getAccession(), Optional.empty(), SAMPLE_CURATED_REPO);
            sample.ifPresent(samples::add);
        }*/

        return sampleList;
    }

    public List<Sample> graphSearch(GraphSearchQuery searchQuery, int limit, int skip) {
        List<Sample> samples = new ArrayList<>();
        GraphSearchQuery response = neoSampleRepository.graphSearch(searchQuery, limit, skip);

        Set<String> accessions = new HashSet<>();
        for (GraphNode node : response.getNodes()) {
            if (node.getType().equalsIgnoreCase("Sample") ) {
                String accession = node.getAttributes().get("accession");
                if (!accessions.contains(accession)) {
                    Optional<Sample> sample = sampleService.fetch(accession, Optional.empty(), SAMPLE_CURATED_REPO);
                    sample.ifPresent(samples::add);
                    accessions.add(accession);
                }
            }
        }

        return samples;
    }

    public GraphSearchQuery graphSearch2(GraphSearchQuery searchQuery, int limit, int skip) {
        return neoSampleRepository.graphSearch(searchQuery, limit, skip);
    }
}
