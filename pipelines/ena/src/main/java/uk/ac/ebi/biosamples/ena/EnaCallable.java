package uk.ac.ebi.biosamples.ena;

import java.io.StringReader;
import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

public class EnaCallable implements Callable<Void> {

	private static final String DDBJ_SAMPLE_PREFIX = "SAMD";

	private static final String NCBI_SAMPLE_PREFIX = "SAMN";

	private static final String SUPPRESSED = "suppressed";

	private Logger log = LoggerFactory.getLogger(getClass());

	private final String sampleAccession;
	private final BioSamplesClient bioSamplesClient;
	private final EnaXmlEnhancer enaXmlEnhancer;
	private final EnaElementConverter enaElementConverter;
	private final EraProDao eraProDao;
	private final String domain;
	private boolean suppressionHandler;

	public EnaCallable(String sampleAccession, BioSamplesClient bioSamplesClient, EnaXmlEnhancer enaXmlEnhancer,
			EnaElementConverter enaElementConverter, EraProDao eraProDao, String domain) {
		this.sampleAccession = sampleAccession;
		this.bioSamplesClient = bioSamplesClient;
		this.enaXmlEnhancer = enaXmlEnhancer;
		this.enaElementConverter = enaElementConverter;
		this.eraProDao = eraProDao;
		this.domain = domain;
	}

	/**
	 * Construction for SUPPRESSED samples
	 * 
	 * @param sampleAccession
	 * @param bioSamplesClient
	 * @param enaXmlEnhancer
	 * @param enaElementConverter
	 * @param eraProDao
	 * @param domain
	 * @param suppressionHandler
	 */
	public EnaCallable(String sampleAccession, BioSamplesClient bioSamplesClient, EnaXmlEnhancer enaXmlEnhancer,
			EnaElementConverter enaElementConverter, EraProDao eraProDao, String domain, boolean suppressionHandler) {
		this.sampleAccession = sampleAccession;
		this.bioSamplesClient = bioSamplesClient;
		this.enaXmlEnhancer = enaXmlEnhancer;
		this.enaElementConverter = enaElementConverter;
		this.eraProDao = eraProDao;
		this.domain = domain;
		this.suppressionHandler = suppressionHandler;
	}

	@Override
	public Void call() throws Exception {
		if (suppressionHandler) {
			return checkAndUpdateSuppressedSample(sampleAccession);
		} else {
			return enrichAndPersistEnaSample();
		}
	}

	/**
	 * Enrich the ENA sample with specific attributes and persist using {@link BioSamplesClient}
	 * 
	 * @return nothing its {@link Void}
	 * 
	 * @throws SQLException if it fails in queries
	 * @throws DocumentException if it fails in XML transformation
	 */
	private Void enrichAndPersistEnaSample() throws SQLException, DocumentException {
		log.info("HANDLING " + sampleAccession);

		String xmlString = eraProDao.getSampleXml(sampleAccession);

		SAXReader reader = new SAXReader();
		Document xml = reader.read(new StringReader(xmlString));
		Element root = enaXmlEnhancer.applyAllRules(xml.getRootElement(),
				enaXmlEnhancer.getEnaDatabaseSample(sampleAccession));

		// check that we got some content
		if (XmlPathBuilder.of(root).path("SAMPLE").exists()) {
			Sample sample = enaElementConverter.convert(root);

			SortedSet<Attribute> attributes = new TreeSet<>(sample.getCharacteristics());
			SortedSet<ExternalReference> externalReferences = new TreeSet<>(sample.getExternalReferences());

			// add dates etc from database
			// add some INSDC things for standardisation with NCBI import
			Instant release = eraProDao.getReleaseDateTime(sampleAccession);
			if (release == null) {
				log.warn("Unable to retrieve release date for " + sampleAccession + " defaulting to now");
				release = Instant.now();
			}
			attributes.add(Attribute.build("INSDC first public", DateTimeFormatter.ISO_INSTANT.format(release)));
			Instant update = eraProDao.getUpdateDateTime(sampleAccession);
			if (update == null) {
				log.warn("Unable to retrieve update date for " + sampleAccession);
			} else {
				attributes.add(Attribute.build("INSDC last update", DateTimeFormatter.ISO_INSTANT.format(update)));
			}

			String checklist = eraProDao.getChecklist(sampleAccession);
			if (checklist == null) {
				log.warn("Unable to retrieve checklist for " + sampleAccession);
			} else {
				attributes.add(Attribute.build("ENA checklist", checklist));
			}

			String status = eraProDao.getStatus(sampleAccession);

			if (status == null) {
				log.warn("Unable to retrieve status for " + sampleAccession);
			} else {
				attributes.add(Attribute.build("INSDC status", status));
			}

			// add external reference
			externalReferences.add(ExternalReference.build("https://www.ebi.ac.uk/ena/data/view/" + sampleAccession));

			sample = Sample.build(sample.getName(), sampleAccession, domain, release, update, attributes,
					sample.getRelationships(), externalReferences);
			bioSamplesClient.persistSampleResource(sample);
		} else {
			log.warn("Unable to find SAMPLE element for " + sampleAccession);
		}
		log.trace("HANDLED " + sampleAccession);
		return null;
	}

	/**
	 * Checks samples from ENA which is SUPPRESSED and takes necessary action, i.e. update status if status is different in BioSamples,
	 * else persist
	 * 
	 * @param sampleAccession
	 * 			The accession passed
	 * @return {@link Void}
	 * @throws InterruptedException if thread is interrupted
	 * @throws SQLException if failure in SQL
	 * @throws DocumentException if failure in document parsing
	 */
	private Void checkAndUpdateSuppressedSample(String sampleAccession)
			throws InterruptedException, SQLException, DocumentException {
		final Optional<Resource<Sample>> optionalSampleResource = bioSamplesClient.fetchSampleResource(sampleAccession, Optional.empty());
		if (optionalSampleResource.isPresent()) {
			final Sample sample = optionalSampleResource.get().getContent();
			boolean persistRequired = true;
			for (Attribute attribute : sample.getAttributes()) {
				if (attribute.getType().equals("INSDC status") && attribute.getValue().equals(SUPPRESSED)) {
					persistRequired = false;
					break;
				}
			}

			if (persistRequired) {
				sample.getAttributes().removeIf(attr -> attr.getType().contains("INSDC status"));
				sample.getAttributes().add(Attribute.build("INSDC status", SUPPRESSED));
				log.info("Updating status to suppressed of sample: " + sampleAccession);
				bioSamplesClient.persistSampleResource(sample);
			}
		} else {
			if (!ifNcbiDdbj(sampleAccession)) {
				log.info("Accession doesn't exist " + sampleAccession + " creating the same");
				return enrichAndPersistEnaSample();
			}
		}

		return null;
	}

	/**
	 * True if NCBI/DDBJ sample
	 * 
	 * @param sampleAccession
	 * 			The accession passed to the method
	 * @return true if NCBI/DDBJ sample
	 */
	private boolean ifNcbiDdbj(String sampleAccession) {
		return sampleAccession.startsWith(NCBI_SAMPLE_PREFIX) || sampleAccession.startsWith(DDBJ_SAMPLE_PREFIX);
	}
}
