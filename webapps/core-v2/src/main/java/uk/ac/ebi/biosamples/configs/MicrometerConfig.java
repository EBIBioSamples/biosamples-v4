package uk.ac.ebi.biosamples.configs;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MicrometerConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> customizeMeterRegistry()
            throws UnknownHostException {
        String hostName = InetAddress.getLocalHost().getHostName();

        return (registry) -> {
            registry.config().commonTags("application", "biosamples-webapps-core-v2", "instance", hostName);
        };
    }
}
