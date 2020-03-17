package uk.ac.ebi.biosamples.ena;

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

import java.io.StringReader;
import java.io.Writer;
import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

public class EnaCallable implements Callable<Void> {
	private static final String DDBJ_SAMPLE_PREFIX = "SAMD";
	private static final String NCBI_SAMPLE_PREFIX = "SAMN";
	private static final String SUPPRESSED = "suppressed";
	private final String sampleAccession;
	private final BioSamplesClient bioSamplesClient;
	private final EnaXmlEnhancer enaXmlEnhancer;
	private final EnaElementConverter enaElementConverter;
	private final EraProDao eraProDao;
	private final String domain;
	private Writer suppListWriter;
	private Logger log = LoggerFactory.getLogger(getClass());
	private boolean suppressionHandler;

	/**
	 * Construction for all ENA samples
	 * 
	 * @param sampleAccession
	 * @param bioSamplesClient
	 * @param enaXmlEnhancer
	 * @param enaElementConverter
	 * @param eraProDao
	 * @param domain
	 */
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
			return checkAndUpdateSuppressedSample();
		} else {
			return enrichAndPersistEnaSample();
		}
	}

	/**
	 * Enrich the ENA sample with specific attributes and persist using
	 * {@link BioSamplesClient}
	 *
	 * @return                   nothing its {@link Void}
	 *
	 * @throws SQLException      if it fails in queries
	 * @throws DocumentException if it fails in XML transformation
	 */
	private Void enrichAndPersistEnaSample() throws SQLException, DocumentException {
		log.info("HANDLING " + sampleAccession);

		final SampleDBBean sampleDBBean = eraProDao.getAllSampleData(this.sampleAccession);
		if (sampleDBBean != null) {
			handleEnaSample(sampleDBBean);
		}

		return null;
	}

	/**
	 * Handles one ENA sample
	 * 
	 * @param  sampleDBBean      {@link SampleDBBean}
	 * 
	 * @throws DocumentException in case of parse errors
	 */
	private void handleEnaSample(final SampleDBBean sampleDBBean) throws DocumentException {
		final String xmlString = sampleDBBean.getSampleXml();
		final SAXReader reader = new SAXReader();
		final Document xml = reader.read(new StringReader(xmlString));
		final Element root = enaXmlEnhancer.applyAllRules(xml.getRootElement(), enaXmlEnhancer.getEnaDatabaseSample(sampleAccession));

		// check that we got some content
		if (XmlPathBuilder.of(root).path("SAMPLE").exists()) {
			enrichEnaSample(sampleDBBean, root);
		} else {
			log.warn("Unable to find SAMPLE element for " + sampleAccession);
		}

		log.trace("HANDLED " + sampleAccession);
	}

	/**
	 * Enriches one ENA sample
	 * 
	 * @param sampleDBBean {@link SampleDBBean}
	 * @param root         The XML {@link Element}
	 */
	private void enrichEnaSample(final SampleDBBean sampleDBBean, final Element root) {
		Sample sample = enaElementConverter.convert(root);
		final SortedSet<Attribute> attributes = new TreeSet<>(sample.getCharacteristics());
		final SortedSet<ExternalReference> externalReferences = new TreeSet<>(sample.getExternalReferences());
		final String lastUpdated = sampleDBBean.getLastUpdate();
		final String firstPublic = sampleDBBean.getFirstPublic();
		final String firstCreated = sampleDBBean.getFirstCreated();
		final String status = handleStatus(sampleDBBean.getStatus());
		Instant release = null;
		Instant update = null;
		Instant create = null;

		if (lastUpdated != null) {
			update = Instant.parse(lastUpdated);
			attributes.add(Attribute.build("INSDC last update", DateTimeFormatter.ISO_INSTANT.format(update)));
		}

		if (firstPublic != null) {
			release = Instant.parse(firstPublic);
			attributes.add(Attribute.build("INSDC first public", DateTimeFormatter.ISO_INSTANT.format(release)));
		} else {
			release = Instant.now();
		}

		if (firstCreated != null) {
			create = Instant.parse(firstCreated);
		}

		if (status != null) {
			attributes.add(Attribute.build("INSDC status", status));
		}

		// add external reference
		externalReferences.add(ExternalReference.build("https://www.ebi.ac.uk/ena/data/view/" + this.sampleAccession));

		// Although update date is passed here, its system generated to time now by
		// webapps-core
		sample = Sample.build(sample.getName(), this.sampleAccession, domain, release, update, create, attributes, sample.getRelationships(),
				externalReferences);
		bioSamplesClient.persistSampleResource(sample);
	}

	private String handleStatus(int statusId) {
		if (1 == statusId) {
			return "draft";
		} else if (2 == statusId) {
			return "private";
		} else if (3 == statusId) {
			return "cancelled";
		} else if (4 == statusId) {
			return "public";
		} else if (5 == statusId) {
			return "suppressed";
		} else if (6 == statusId) {
			return "killed";
		} else if (7 == statusId) {
			return "temporary_suppressed";
		} else if (8 == statusId) {
			return "temporary_killed";
		}

		throw new RuntimeException("Unrecognised statusid " + statusId);
	}

	/**
	 * Checks samples from ENA which is SUPPRESSED and takes necessary action, i.e.
	 * update status if status is different in BioSamples, else persist
	 *
	 * @return                      {@link Void}
	 * @throws InterruptedException if thread is interrupted
	 * @throws SQLException         if failure in SQL
	 * @throws DocumentException    if failure in document parsing
	 */
	private Void checkAndUpdateSuppressedSample() throws InterruptedException, SQLException, DocumentException {
		final Optional<Resource<Sample>> optionalSampleResource = bioSamplesClient.fetchSampleResource(this.sampleAccession,
				Optional.of(new ArrayList<String>()));

		if (optionalSampleResource.isPresent()) {
			final Sample sample = optionalSampleResource.get().getContent();

			writeToSuppressedSamplesFile(sample);

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
				log.info("Updating status to suppressed of sample: " + this.sampleAccession);
				bioSamplesClient.persistSampleResource(sample);
			}
		} else {
			if (!ifNcbiDdbj()) {
				log.info("Accession doesn't exist " + this.sampleAccession + " creating the same");
				return enrichAndPersistEnaSample();
			}
		}

		return null;
	}

	private void writeToSuppressedSamplesFile(Sample sample) {
		if (Instant.now().isAfter(sample.getRelease())) {
			try {
				suppListWriter.write(sample.getAccession());
				suppListWriter.write("\n");
				suppListWriter.flush();
			} catch (final Exception e) {
				log.error("Exception in supplist building");
			}
		}
	}

	/**
	 * True if NCBI/DDBJ sample
	 *
	 * @return                 true if NCBI/DDBJ sample
	 */
	private boolean ifNcbiDdbj() {
		return this.sampleAccession.startsWith(NCBI_SAMPLE_PREFIX) || this.sampleAccession.startsWith(DDBJ_SAMPLE_PREFIX);
	}
}
