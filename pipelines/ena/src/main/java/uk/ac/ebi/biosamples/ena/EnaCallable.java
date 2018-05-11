package uk.ac.ebi.biosamples.ena;

import java.io.StringReader;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

public class EnaCallable implements Callable<Void> {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final String sampleAccession;
	private final BioSamplesClient bioSamplesClient;
	private final EnaElementConverter enaElementConverter;
	private final EraProDao eraProDao;
	private final String domain;
	
	public EnaCallable(String sampleAccession, BioSamplesClient bioSamplesClient,
			EnaElementConverter enaElementConverter, EraProDao eraProDao, String domain) {
		this.sampleAccession = sampleAccession;
		this.bioSamplesClient = bioSamplesClient;
		this.enaElementConverter = enaElementConverter;
		this.eraProDao = eraProDao;
		this.domain = domain;
	}

	@Override
	public Void call() throws Exception {
		log.trace("HANDLING " + sampleAccession);
		
		String xmlString = eraProDao.getSampleXml(sampleAccession);
		// System.out.println(xmlString);
		SAXReader reader = new SAXReader();
		Document xml = reader.read(new StringReader(xmlString));
		Element root = xml.getRootElement();
		// check that we got some content
		if (XmlPathBuilder.of(root).path("SAMPLE").exists()) {
			Sample sample = enaElementConverter.convert(root);
			
			SortedSet<Attribute> attributes = new TreeSet<>(sample.getCharacteristics());
			SortedSet<ExternalReference> externalReferences = new TreeSet<>(sample.getExternalReferences());
			
			// add dates etc from database
			//add some INSDC things for standardisation with NCBI import
			Instant release = eraProDao.getReleaseDateTime(sampleAccession);
			if (release == null) {
				log.warn("Unable to retrieve release date for "+sampleAccession);
			} else {
				attributes.add(Attribute.build("INSDC first public", 
					DateTimeFormatter.ISO_INSTANT.format(release)));
			}
			Instant update = eraProDao.getUpdateDateTime(sampleAccession);
			if (update == null) {
				log.warn("Unable to retrieve update date for "+sampleAccession);
			} else {
				attributes.add(Attribute.build("INSDC last update", 
					DateTimeFormatter.ISO_INSTANT.format(update)));
			}
			String centreName = eraProDao.getCentreName(sampleAccession);
			if (centreName == null) {
				log.warn("Unable to retrieve centre name for "+sampleAccession);
			} else {
				attributes.add(Attribute.build("INSDC center name", centreName));				
			}
			String checklist = eraProDao.getChecklist(sampleAccession);
			if (checklist == null) {
				log.warn("Unable to retrieve checklist for "+sampleAccession);
			} else {
				attributes.add(Attribute.build("ENA checklist", checklist));
			}
			String status = eraProDao.getStatus(sampleAccession);
			if (status == null) {
				log.warn("Unable to retrieve status for "+sampleAccession);
			} else {
				attributes.add(Attribute.build("INSDC status", status));
			}
			
			//add external reference
			externalReferences.add(ExternalReference.build("https://www.ebi.ac.uk/ena/data/view/"+sampleAccession));

			sample = Sample.build(sample.getName(), sampleAccession, domain, release, update, attributes,
					sample.getRelationships(), externalReferences);
			
			bioSamplesClient.persistSampleResource(sample);
		} else {
			log.warn("Unable to find SAMPLE element for " + sampleAccession);
		}
		log.trace("HANDLED " + sampleAccession);
		return null;
	}

}
