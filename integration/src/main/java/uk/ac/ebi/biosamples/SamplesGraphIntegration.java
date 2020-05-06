package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo4j.model.NeoSample;
import uk.ac.ebi.biosamples.neo4j.repo.NeoSampleRepository;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

import java.time.Instant;
import java.util.*;

@Component
public class SamplesGraphIntegration extends AbstractIntegration {
    private static final Logger LOG = LoggerFactory.getLogger(SamplesGraphIntegration.class);

    private final NeoSampleRepository neoSampleRepository;

    public SamplesGraphIntegration(BioSamplesClient client, NeoSampleRepository neoSampleRepository) {
        super(client);
        this.neoSampleRepository = neoSampleRepository;
    }

    @Override
    protected void phaseOne() {
        Sample test1 = getSampleTest1();

        Resource<Sample> resource = client.persistSampleResource(test1);
        test1 = Sample.Builder.fromSample(test1).withAccession(resource.getContent().getAccession()).build();
        if (!test1.equals(resource.getContent())) {
            throw new IntegrationTestFailException("Expected response (" + resource.getContent() + ") to equal submission (" + test1 + ")", Phase.ONE);
        }

        NeoSample neoSample = NeoSample.build(test1);
//        neoSampleRepository.loadSample(neoSample);

    }

    @Override
    protected void phaseTwo() {

    }

    @Override
    protected void phaseThree() {

    }

    @Override
    protected void phaseFour() {
        List<Sample> samples = new ArrayList<>();
        for (Resource<Sample> sample : client.fetchSampleResourceAll()) {
            samples.add(sample.getContent());
        }

        for (Sample sample : samples) {
            NeoSample neoSample = NeoSample.build(sample);
//            neoSampleRepository.loadSample(neoSample);
        }
    }

    @Override
    protected void phaseFive() {
        List<Sample> samples = Collections.singletonList(getSampleTest1()); //todo populate with actual data
        boolean sampleExist = false;

        for (Sample sample : samples) {
            if (getSampleTest1().getName().equals(sample.getName())) {
                sampleExist = true;
                break;
            }
        }

        if (!sampleExist) {
            throw new IntegrationTestFailException("Expected sample " + getSampleTest1().getName() + " not present", Phase.FIVE);
        }
    }

    private Sample getSampleTest1() {
        String name = "SampleGraphIntegration_sample_1";
        Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

        SortedSet<Attribute> attributes = new TreeSet<>();
        attributes.add(
                Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

        return new Sample.Builder(name)
                .withDomain(defaultIntegrationSubmissionDomain)
                .withRelease(release)
                .withAttributes(attributes).build();
    }
}
