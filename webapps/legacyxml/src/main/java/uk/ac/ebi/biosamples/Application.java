package uk.ac.ebi.biosamples;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MarshallingHttpMessageConverter;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleAsXMLHttpMessageConverter;
import uk.ac.ebi.biosamples.service.SampleToXmlConverter;

import javax.xml.bind.Marshaller;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {



	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
	@Bean
	public HttpMessageConverter<Sample> getXmlSampleHttpMessageConverter(SampleToXmlConverter sampleToXmlConverter) {
		return new SampleAsXMLHttpMessageConverter(sampleToXmlConverter);
	}


	/* Necessary to render the BioSamples XML using the format and comments */
	@Bean(name = "marshallingHttpMessageConverter")
	public MarshallingHttpMessageConverter getMarshallingHttpMessageConverter() {
		MarshallingHttpMessageConverter marshallingHttpMessageConverter = new MarshallingHttpMessageConverter();
		marshallingHttpMessageConverter.setMarshaller(getJaxb2Marshaller());
		marshallingHttpMessageConverter.setUnmarshaller(getJaxb2Marshaller());
		return marshallingHttpMessageConverter;
	}

	/* Necessary to render the BioSamples XML using the format and comments */
	@Bean(name = "jaxb2Marshaller")
	public Jaxb2Marshaller getJaxb2Marshaller() {
		String documentComment = "BioSamples XML API - version 1.0";

		Map<String, Object> props = new HashMap<>();
		props.put(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		props.put(Marshaller.JAXB_SCHEMA_LOCATION,"http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0 http://www.ebi.ac.uk/biosamples/assets/xsd/v1.0/ResultQuerySampleSchema.xsd");
		props.put("com.sun.xml.internal.bind.xmlHeaders", String.format("\n<!-- %s -->", documentComment));
		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		jaxb2Marshaller.setPackagesToScan("uk.ac.ebi.biosamples.model");
		jaxb2Marshaller.setMarshallerProperties(props);

		return jaxb2Marshaller;
	}

	/* Necessary to render XML using Jaxb annotations*/
	@Bean
	public Jaxb2RootElementHttpMessageConverter getJaxb2RootElementHttpMessageConverter() {
		return new Jaxb2RootElementHttpMessageConverter();
	}

//	@Bean
//	public FilterRegistrationBean filterRegistrationBean() {
//		CharacterEncodingFilter filter = new CharacterEncodingFilter();
//		filter.setEncoding("UTF-8");
//
//		FilterRegistrationBean registrationBean = new FilterRegistrationBean();
//		registrationBean.setFilter(filter);
//		registrationBean.addUrlPatterns("/*");
//		return registrationBean;
//	}



}
