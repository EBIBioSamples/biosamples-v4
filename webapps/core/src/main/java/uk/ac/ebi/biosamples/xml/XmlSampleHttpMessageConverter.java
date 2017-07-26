package uk.ac.ebi.biosamples.xml;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleToXmlConverter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
//public class XmlSampleHttpMessageConverter implements HttpMessageConverter<Sample> {
public class XmlSampleHttpMessageConverter extends AbstractHttpMessageConverter<Sample> {

	@Autowired
	private SampleToXmlConverter sampleToXmlConverter;

	private final List<MediaType> DEFAULT_SUPPORTED_MEDIA_TYPES = Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML);

	public XmlSampleHttpMessageConverter() {
		this.setSupportedMediaTypes(this.DEFAULT_SUPPORTED_MEDIA_TYPES);
	}

//	@Override
//	public boolean canRead(Class<?> clazz, MediaType mediaType) {
//		return Sample.class.isAssignableFrom(clazz) && getSupportedMediaTypes().contains(mediaType);
//	}
//
//	@Override
//	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
//		return Sample.class.isAssignableFrom(clazz) && getSupportedMediaTypes().contains(mediaType);
//	}
//
//	@Override
//	public List<MediaType> getSupportedMediaTypes() {
////		return Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
//		return Collections.singletonList(MediaType.TEXT_XML);
//	}

//	@Override
//	public Sample read(Class<? extends Sample> clazz, HttpInputMessage inputMessage)
//			throws IOException, HttpMessageNotReadableException {
//		throw new RuntimeException("Not implemented yet");
//	}
//
//	@Override
//	public void write(Sample sample, MediaType contentType, HttpOutputMessage outputMessage)
//			throws IOException, HttpMessageNotWritableException {
//		Document doc = sampleToXmlConverter.convert(sample);
//		OutputFormat format = OutputFormat.createCompactFormat();
//		XMLWriter writer = new XMLWriter(outputMessage.getBody(), format);
//		writer.write(doc);
//	}

		@Override
		protected boolean supports(Class<?> clazz) {
			return Sample.class.isAssignableFrom(clazz);
		}

		@Override
		protected Sample readInternal(Class<? extends Sample> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
			return null;
		}

		@Override
		protected void writeInternal(Sample sample, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
            Document doc = sampleToXmlConverter.convert(sample);
            OutputFormat format = OutputFormat.createCompactFormat();
            XMLWriter writer = new XMLWriter(outputMessage.getBody(), format);
            writer.write(doc);
		}

}
