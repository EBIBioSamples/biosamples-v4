package uk.ac.ebi.biosamples.service;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Service;
import org.xml.sax.XMLReader;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleToXmlConverter;
import uk.ac.ebi.biosamples.service.XmlToSampleConverter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class XmlSampleHttpMessageConverter extends AbstractHttpMessageConverter<Sample> {
	
	private final SampleToXmlConverter sampleToXmlConverter;
	private final XmlToSampleConverter xmlToSampleConverter;
	
	private final List<MediaType> DEFAULT_SUPPORTED_MEDIA_TYPES = Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML);

	private Logger log = LoggerFactory.getLogger(getClass());

	public XmlSampleHttpMessageConverter(SampleToXmlConverter sampleToXmlConverter, XmlToSampleConverter xmlToSampleConverter) {
		this.setSupportedMediaTypes(this.DEFAULT_SUPPORTED_MEDIA_TYPES);
		this.sampleToXmlConverter = sampleToXmlConverter;
		this.xmlToSampleConverter = xmlToSampleConverter;
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return Sample.class.isAssignableFrom(clazz);
	}

	@Override
	protected Sample readInternal(Class<? extends Sample> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		
		log.info("Reached readInternal");
		
		SAXReader saxReader = new SAXReader();
		Document doc;
		try {
			doc = saxReader.read(inputMessage.getBody());
		} catch (DocumentException e) {
			throw new HttpMessageNotReadableException("error parsing xml", e);
		}
		Sample sample = xmlToSampleConverter.convert(doc);
		return sample;
	}

	@Override
	protected void writeInternal(Sample sample, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		Document doc = sampleToXmlConverter.convert(sample);
		OutputFormat format = OutputFormat.createCompactFormat();
		XMLWriter writer = new XMLWriter(outputMessage.getBody(), format);
		writer.write(doc);
	}

}
