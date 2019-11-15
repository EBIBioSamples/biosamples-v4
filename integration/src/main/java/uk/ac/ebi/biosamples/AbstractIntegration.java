package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;

import org.springframework.hateoas.Resource;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

public abstract class AbstractIntegration implements ApplicationRunner, ExitCodeGenerator {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    protected final String defaultIntegrationSubmissionDomain = "self.BiosampleIntegrationTest";
    protected int exitCode = 1;
    protected final BioSamplesClient client;

    protected abstract void phaseOne();

    protected abstract void phaseTwo();

    protected abstract void phaseThree();

    protected abstract void phaseFour();

    protected abstract void phaseFive();

    public AbstractIntegration(BioSamplesClient client) {
        this.client = client;
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Phase phase = Phase.readPhaseFromArguments(args);
        switch (phase) {
            case ONE:
                phaseOne();
                break;
            case TWO:
                phaseTwo();
                break;
            case THREE:
                phaseThree();
                break;
            case FOUR:
                phaseFour();
                break;
            case FIVE:
                phaseFive();
                break;
            default:
                log.warn("Invalid integration test phase {}", phase);
                break;
        }

        close();
        exitCode = 0;
    }

    public void close() {
        //do nothing
    }

    //For the purpose of unit testing we will consider name is unique, so we can fetch sample uniquely from name
    Optional<Sample> fetchUniqueSampleByName(String name) {
        Optional<Sample> optionalSample;
        Filter nameFilter = FilterBuilder.create().onName(name).build();
        Iterator<Resource<Sample>> resourceIterator = client.fetchSampleResourceAll(Collections.singletonList(nameFilter)).iterator();

        if (resourceIterator.hasNext()) {
            optionalSample = Optional.of(resourceIterator.next().getContent());
        } else {
            optionalSample = Optional.empty();
        }

        if (resourceIterator.hasNext()) {
            throw new IntegrationTestFailException("More than one sample present with the given name");
        }

        return optionalSample;
    }

}
