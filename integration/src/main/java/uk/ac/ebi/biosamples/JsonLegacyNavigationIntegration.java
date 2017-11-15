package uk.ac.ebi.biosamples;

import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import uk.ac.ebi.biosamples.client.BioSamplesClient;

import java.util.HashMap;
import java.util.Map;

public class JsonLegacyNavigationIntegration extends AbstractIntegration{

    private final BioSamplesProperties bioSamplesProperties;

    public JsonLegacyNavigationIntegration(BioSamplesProperties biosamplesProperties, BioSamplesClient client) {
        super(client);
        this.bioSamplesProperties = biosamplesProperties;
    }

    @Override
    protected void phaseOne() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("text","TESTrestfacet1");
        Traverson traverson = new Traverson(bioSamplesProperties.getBiosamplesClientUri(), MediaTypes.HAL_JSON);
        Traverson.TraversalBuilder builder = traverson.follow("samples", "facet").withTemplateParameters(parameters);

    }

    @Override
    protected void phaseTwo() {

    }

    @Override
    protected void phaseThree() {

    }

    @Override
    protected void phaseFour() {

    }

    @Override
    protected void phaseFive() {

    }
}
