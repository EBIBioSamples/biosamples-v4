package uk.ac.ebi.biosamples.template;

import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.PipelineApplicationRunner;
import uk.ac.ebi.biosamples.PipelineSampleCallable;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
//import uk.ac.ebi.biosamples.service.AnalyticsService;

@Component
public class PipelineTemplateApplicationRunner extends PipelineApplicationRunner {
    private static final String PIPELINE_NAME = "SAMPLE_TEST";
    private String domain;

    public PipelineTemplateApplicationRunner(BioSamplesClient bioSamplesClient,
                                             PipelinesProperties pipelinesProperties/*,
                                             AnalyticsService analyticsService*/) {
        super(bioSamplesClient, pipelinesProperties/*, analyticsService*/);
    }

    @Override
    public void loadPreConfiguration() {
        this.domain = "self.testDomain";
        LOG.info("Loading pre configurations for {}", PIPELINE_NAME);
    }

    @Override
    public PipelineSampleCallable getNewCallableClassInstance() {
        return new SampleTemplateCallable(bioSamplesClient, domain);
    }

    protected String getPipelineName() {
        return PIPELINE_NAME;
    }
}
