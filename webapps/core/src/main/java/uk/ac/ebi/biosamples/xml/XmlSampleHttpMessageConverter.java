package uk.ac.ebi.biosamples.xml;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleToXmlConverter;

@Service
public class XmlSampleHttpMessageConverter implements HttpMessageConverter<Sample> {

	@Autowired
	private SampleToXmlConverter sampleToXmlConverter;
	
	public XmlSampleHttpMessageConverter() {
	}
	

	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		return Sample.class.isAssignableFrom(clazz) && getSupportedMediaTypes().contains(mediaType);
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return Sample.class.isAssignableFrom(clazz) && getSupportedMediaTypes().contains(mediaType);
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return Collections.singletonList(MediaType.APPLICATION_XML);
	}

	@Override
	public Sample read(Class<? extends Sample> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public void write(Sample sample, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		Document doc = sampleToXmlConverter.convert(sample);
		OutputFormat format = OutputFormat.createCompactFormat();
		XMLWriter writer = new XMLWriter( outputMessage.getBody(), format );
        writer.write( doc );		
	}

}
