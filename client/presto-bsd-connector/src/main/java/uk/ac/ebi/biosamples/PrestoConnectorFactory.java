package uk.ac.ebi.biosamples;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.airlift.configuration.InvalidConfigurationException;
import io.prestosql.spi.connector.Connector;
import io.prestosql.spi.connector.ConnectorContext;
import io.prestosql.spi.connector.ConnectorFactory;
import io.prestosql.spi.connector.ConnectorHandleResolver;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.service.AapClientService;
import uk.ac.ebi.biosamples.schema.PrestoSchemaMetadata;
import uk.ac.ebi.biosamples.service.AttributeValidator;
import uk.ac.ebi.biosamples.service.SampleValidator;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class PrestoConnectorFactory implements ConnectorFactory {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public String getName() {
        return "biosamples";
    }

    @Override
    public ConnectorHandleResolver getHandleResolver() {
        return new PrestoHandleResolver();
    }

    @Override
    public Connector create(String catalogName, Map<String, String> requiredConfig, ConnectorContext context) {
        requireNonNull(requiredConfig, "requiredConfig is null");

        PrestoConfig prestoConfig = null;
        BioSamplesClient client = null;
        BioSamplesProperties bioSamplesProperties = new BioSamplesProperties();
        try {
            prestoConfig = new PrestoConfig(requiredConfig);

            SampleValidator sampleValidator = new SampleValidator(new AttributeValidator());
            AapClientService aapClientService = null;


            RestTemplate restTemplate = new RestTemplate(
                    new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));

            //add converter for hal processing as the first converter
            List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new Jackson2HalModule());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            MappingJackson2HttpMessageConverter halConverter =
                    new TypeConstrainedMappingJackson2HttpMessageConverter(ResourceSupport.class);
            halConverter.setObjectMapper(mapper);
            halConverter.setSupportedMediaTypes(Collections.singletonList(MediaTypes.HAL_JSON));
            converters.add(0, halConverter);
            restTemplate.setMessageConverters(converters);

            FieldUtils.writeField(bioSamplesProperties, "biosamplesClientUri", prestoConfig.getBioSamplesClientUri(), true);
            FieldUtils.writeField(bioSamplesProperties, "biosamplesClientPagesize", 5, true);
            FieldUtils.writeField(bioSamplesProperties, "biosamplesClientTimeout", 60000, true);
            FieldUtils.writeField(bioSamplesProperties, "connectionCountMax", 8, true);
            FieldUtils.writeField(bioSamplesProperties, "connectionCountDefault", 8, true);
            FieldUtils.writeField(bioSamplesProperties, "threadCount", 1, true);
            FieldUtils.writeField(bioSamplesProperties, "threadCountMax", 8, true);
            FieldUtils.writeField(bioSamplesProperties, "biosamplesClientAapUri", prestoConfig.getBioSamplesAapUri(), true);
//            FieldUtils.writeField(bioSamplesProperties, "biosamplesClientAapUsername", "x", true);
//            FieldUtils.writeField(bioSamplesProperties, "biosamplesClientAapPassword", "x", true);
            FieldUtils.writeField(bioSamplesProperties, "biosamplesClientCacheMaxEntries", 0, true);
            FieldUtils.writeField(bioSamplesProperties, "biosamplesClientCacheMaxObjectSize", 1048576, true);


            client = new BioSamplesClient(prestoConfig.getBioSamplesClientUri(), restTemplate,
                    sampleValidator, aapClientService, bioSamplesProperties);
        } catch (InvalidConfigurationException | IllegalAccessException e) {
            logger.error("Failed to create biosamples-client", e);
        }


        PrestoConnectorId connectorId = new PrestoConnectorId(catalogName);
        PrestoSchemaMetadata schemaMetadata = new PrestoSchemaMetadata(prestoConfig);
        PrestoMetadata metadata = new PrestoMetadata(connectorId, schemaMetadata);
        PrestoSplitManager splitManager = new PrestoSplitManager(connectorId, schemaMetadata);
        PrestoRecordSetProvider recordSetProvider = new PrestoRecordSetProvider(connectorId, client, metadata);
        PrestoPageSourceProvider prestoPageSourceProvider = new PrestoPageSourceProvider(recordSetProvider, bioSamplesProperties.getBiosamplesClientPagesize());

        return new PrestoConnector(metadata, splitManager, recordSetProvider, prestoPageSourceProvider);
    }

}
