package uk.ac.ebi.biosamples;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {



	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}



	 // @Override
	 //  protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
	    /*
		// https://github.com/spring-projects/spring-boot/issues/2745
		// The error page on Tomcat deployed application interfere with the
		// proper error handling provided by the default error handler
		//set register error pagefilter false
		setRegisterErrorPageFilter(false);
		builder.sources(Application.class);
		return builder;
	    */

	     // Properties properties = new Properties();
	     // properties.setProperty("spring.jackson.serialization.indent_output", "true");
	     // builder.properties(properties);
	     // return builder;
	 //  }



	// If you
//	@Bean
//	public Jaxb2RootElementHttpMessageConverter getJaxb2RootElementHttpMessageConverter() {
//		Jaxb2RootElementHttpMessageConverter converter = new Jaxb2RootElementHttpMessageConverter();
//		converter.setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML));
//		return converter;
//	}


	@Bean(name = "marshallingHttpMessageConverter")
	public MarshallingHttpMessageConverter getMarshallingHttpMessageConverter() {
		MarshallingHttpMessageConverter marshallingHttpMessageConverter = new MarshallingHttpMessageConverter();
		marshallingHttpMessageConverter.setMarshaller(getJaxb2Marshaller());
		marshallingHttpMessageConverter.setUnmarshaller(getJaxb2Marshaller());
		return marshallingHttpMessageConverter;
	}

	@Bean(name = "jaxb2Marshaller")
	public Jaxb2Marshaller getJaxb2Marshaller() {
		String documentComment = "BioSamples XML API - version 1.0";

		Map<String, Object> props = new HashMap<>();
		props.put(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		props.put(Marshaller.JAXB_SCHEMA_LOCATION,"http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0 http://www.ebi.ac.uk/biosamples/assets/xsd/v1.0/ResultQuerySampleSchema.xsd");
		props.put("com.sun.xml.internal.bind.xmlHeaders", String.format("\n<!-- %s -->", documentComment));
		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		jaxb2Marshaller.setClassesToBeBound(CustomXmlError.class,
				BioSampleGroupResultQuery.class,
				BioSampleResultQuery.class);
		jaxb2Marshaller.setMarshallerProperties(props);

		return jaxb2Marshaller;
	}

}
