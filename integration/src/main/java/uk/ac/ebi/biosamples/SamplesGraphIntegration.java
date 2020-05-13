package uk.ac.ebi.biosamples;

import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo4j.model.GraphNode;
import uk.ac.ebi.biosamples.neo4j.model.GraphSearchQuery;
import uk.ac.ebi.biosamples.neo4j.model.NeoSample;
import uk.ac.ebi.biosamples.neo4j.repo.NeoSampleRepository;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class SamplesGraphIntegration extends AbstractIntegration {
    private final NeoSampleRepository neoSampleRepository;

    public SamplesGraphIntegration(BioSamplesClient client, NeoSampleRepository neoSampleRepository) {
        super(client);
        this.neoSampleRepository = neoSampleRepository;
    }

    @Override
    protected void phaseOne() {
        // nothing to test here
    }

    @Override
    protected void phaseTwo() {
        // nothing to test here
    }

    @Override
    protected void phaseThree() {
        // nothing to test here
    }

    @Override
    protected void phaseFour() {
        List<Sample> samples = new ArrayList<>();
        for (Resource<Sample> sample : client.fetchSampleResourceAll()) {
            samples.add(sample.getContent());
        }

        for (Sample sample : samples) {
            NeoSample neoSample = NeoSample.build(sample);
            neoSampleRepository.loadSample(neoSample);
        }
    }

    @Override
    protected void phaseFive() {
        GraphSearchQuery query = new GraphSearchQuery();
        GraphNode node = new GraphNode();
        node.setId("a1");
        node.setType("Sample");
        node.setAttributes(Map.of("organism", "homo sapiens"));
        query.setNodes(List.of(node));
        query.setLinks(Collections.emptyList());

        GraphSearchQuery response = neoSampleRepository.graphSearch(query, 10, 10);
        if (response.getNodes().isEmpty()) {
            throw new IntegrationTestFailException("No samples present in neo4j", Phase.FIVE);
        }
    }
}
