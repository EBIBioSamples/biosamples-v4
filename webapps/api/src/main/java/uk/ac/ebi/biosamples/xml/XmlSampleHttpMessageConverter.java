package uk.ac.ebi.biosamples.xml;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.models.Attribute;
import uk.ac.ebi.biosamples.models.Relationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.xml.samplegroupexport.BioSampleType;
import uk.ac.ebi.biosamples.xml.samplegroupexport.PropertyType;
import uk.ac.ebi.biosamples.xml.samplegroupexport.QualifiedValueType;

@Service
public class XmlSampleHttpMessageConverter implements HttpMessageConverter<MongoSample> {

	private JAXBContext jaxbContext;
	private Unmarshaller jaxbUnmarshaller;
	private Marshaller jaxbMarshaller;

	public XmlSampleHttpMessageConverter() {
	}

	@PostConstruct
	protected void setup() throws JAXBException {
		jaxbContext = JAXBContext.newInstance(BioSampleType.class);
		jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

	}

	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		return MongoSample.class.isAssignableFrom(clazz);
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return MongoSample.class.isAssignableFrom(clazz);
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return Collections.singletonList(MediaType.APPLICATION_XML);
	}

	@Override
	public MongoSample read(Class<? extends MongoSample> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		// use JAXB to turn into a collection of java objects
		BioSampleType biosample;
		try {
			biosample = (BioSampleType) jaxbUnmarshaller.unmarshal(inputMessage.getBody());
		} catch (JAXBException e) {
			throw new HttpMessageNotReadableException("Unable to unmarshal input", e);
		}

		// now converter the XML-based java objects to the JSON-based
		// MongoSample object

		String accession = biosample.getId();
		LocalDateTime releaseDateTime = LocalDateTime.parse(biosample.getSubmissionReleaseDate(),
				DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		LocalDateTime updateDateTime = LocalDateTime.parse(biosample.getSubmissionUpdateDate(),
				DateTimeFormatter.ISO_LOCAL_DATE_TIME);

		String name = null;
		SortedSet<Relationship> relationships = new TreeSet<>();
		SortedSet<Attribute> attributes = new TreeSet<>();

		for (PropertyType propertyType : biosample.getProperty()) {
			if (propertyType.getClazz().equals("Sample Name")) {
				name = propertyType.getQualifiedValue().get(0).getValue();
			} else {
				for (QualifiedValueType propertyValue : propertyType.getQualifiedValue()) {
					// TODO: include group accessions here too?
					if (propertyValue.getValue().matches("SAM[END][GA]?[0-9]+")) {
						// assume its a relationship
						relationships
								.add(Relationship.build(propertyType.getClazz(), propertyValue.getValue(), accession));
					} else {
						// TODO handle term source
						attributes.add(Attribute.build(propertyType.getClazz(), propertyValue.getValue(), null,
								propertyValue.getUnit()));
					}
				}
			}
		}
		for (String derivedFrom : biosample.getDerivedFrom()) {
			relationships.add(Relationship.build("derived from", derivedFrom, accession));
		}

		// TODO external references

		if (name == null) {
			throw new HttpMessageNotReadableException("'Sample Name' must not be null");
		}

		return MongoSample.build(name, accession, releaseDateTime, updateDateTime, attributes, relationships);
	}

	@Override
	public void write(MongoSample sample, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		BioSampleType biosample = new BioSampleType();
		biosample.setId(sample.getAccession());
		biosample.setSubmissionReleaseDate(sample.getRelease().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		biosample.setSubmissionUpdateDate(sample.getUpdate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

		for (Attribute attribute : sample.getAttributes()) {
			PropertyType propertyType = null;
			// find if there is an existing property with this type
			for (PropertyType propertyTypeTest : biosample.getProperty()) {
				if (propertyTypeTest.getClazz().equals(attribute.getKey())) {
					propertyType = propertyTypeTest;
					break;
				}
			}
			if (propertyType == null) {
				propertyType = new PropertyType();
				propertyType.setClazz(attribute.getKey());
				// hard-code some of these
				// might not be right, but not clear if anyone uses these
				// anyway...
				propertyType.setCharacteristic(false);
				propertyType.setComment(false);
				propertyType.setType("STRING");
			}
		}
		// TODO relationships
		// TODO derived from
		// TODO external references

		try {
			jaxbMarshaller.marshal(biosample, outputMessage.getBody());
		} catch (JAXBException e) {
			throw new HttpMessageNotWritableException("Unable to marshal input", e);
		}
		
	}

}
