package uk.ac.ebi.biosamples;

import io.airlift.configuration.InvalidConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class PrestoConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrestoConfig.class);

    private final URI bioSamplesClientUri;
    private URI bioSamplesAapUri;

    public PrestoConfig(Map<String, String> properties) throws InvalidConfigurationException {
        requireNonNull(properties.get("biosamples.client.uri"), "biosamples.client.uri is required");
        try {
            this.bioSamplesClientUri = new URI(properties.get("biosamples.client.uri"));
        } catch (Exception e) {
            LOGGER.error("Invalid client URI", e);
            throw new InvalidConfigurationException("Invalid BioSample client URI");
        }

        try {
            this.bioSamplesAapUri = new URI(properties.get("biosamples.aap.uri"));
        } catch (URISyntaxException e) {
            LOGGER.warn("Invalid BioSamples AAP URI", e);
        }
    }

    @NotNull
    public URI getBioSamplesClientUri() {
        return bioSamplesClientUri;
    }

    @NotNull
    public URI getBioSamplesAapUri() {
        return bioSamplesAapUri;
    }
}
