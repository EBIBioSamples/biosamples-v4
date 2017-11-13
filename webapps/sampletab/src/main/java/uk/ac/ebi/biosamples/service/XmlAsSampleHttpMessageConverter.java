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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class XmlAsSampleHttpMessageConverter extends AbstractHttpMessageConverter<Sample> {
	
	private final XmlSampleToSampleConverter xmlSampleToSampleConverter;
	private final XmlGroupToSampleConverter xmlGroupToSampleConverter;
	
	private final List<MediaType> DEFAULT_SUPPORTED_MEDIA_TYPES = Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML);

	private Logger log = LoggerFactory.getLogger(getClass());

	public XmlAsSampleHttpMessageConverter(XmlSampleToSampleConverter xmlSampleToSampleConverter, 
			XmlGroupToSampleConverter xmlGroupToSampleConverter) {
		this.setSupportedMediaTypes(this.DEFAULT_SUPPORTED_MEDIA_TYPES);
		this.xmlSampleToSampleConverter = xmlSampleToSampleConverter;
		this.xmlGroupToSampleConverter = xmlGroupToSampleConverter;
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
		
		if (doc.getRootElement().getName().equals("BioSample")) {
			log.info("converting BioSample");
			return xmlSampleToSampleConverter.convert(doc);
		} else if (doc.getRootElement().getName().equals("BioSampleGroup")) {
			log.info("converting BioSampleGroup");
			return xmlGroupToSampleConverter.convert(doc);
		} else {
			log.error("Unable to read message with root element "+doc.getRootElement().getName());
			throw new HttpMessageNotReadableException("Cannot recognize xml"); 
		}
	}

	@Override
	protected void writeInternal(Sample sample, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		throw new HttpMessageNotReadableException("Cannot write xml"); 
	}

}
