package uk.ac.ebi.biosamples;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class WebConfiguration extends WebMvcConfigurerAdapter{

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.mediaType("ldjson", new MediaType("application", "ld+json"));
        configurer.mediaType("haljson", new MediaType("application", "hal+json"));
        configurer.mediaType("pxf", new MediaType("application", "phenopacket+json"));

    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/schemas/**")
                .addResourceLocations("classpath:/schemas/");
    }
}
