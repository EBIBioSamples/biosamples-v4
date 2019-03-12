package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class PrestoConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrestoConfig.class);

    private URI bioSamplesClientUri;

    public PrestoConfig(Map<String, String> properties) {
        requireNonNull(properties.get("biosamples.client.uri"), "biosamples.client.uri is required");
        try {
            this.bioSamplesClientUri = new URI(properties.get("biosamples.client.uri"));
        } catch (Exception e) {
            LOGGER.error("Invalid client URI", e);
        }
    }

    @NotNull
    public URI getBioSamplesClientUri() {
        return bioSamplesClientUri;
    }

    public void BioSamplesClientUri(URI bioSamplesClientUri) {
        this.bioSamplesClientUri = bioSamplesClientUri;
    }
}
