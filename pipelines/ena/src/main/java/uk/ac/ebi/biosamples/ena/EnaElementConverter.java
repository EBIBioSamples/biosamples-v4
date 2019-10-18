package uk.ac.ebi.biosamples.ena;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.TaxonomyService;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

@Service
public class EnaElementConverter implements Converter<Element, Sample> {
	private static final String ALIAS = "alias";
	// Fields required by ENA content
	private static final String ENA_SRA_ACCESSION = "SRA accession";
	private static final String ENA_BROKER_NAME = "Broker name";
	private static final String INSDC_CENTER_NAME = "INSDC center name";
	private static final String INSDC_CENTER_ALIAS = "INSDC center alias";
	private static final String ENA_TITLE = "Title";
	private static final String ENA_DESCRIPTION = "Description";
	private static final String SAMPLE = "SAMPLE";
	private static final String IDENTIFIERS = "IDENTIFIERS";
	private static final String PRIMARY_ID = "PRIMARY_ID";
	private static final String SUBMITTER_ID = "SUBMITTER_ID";
	private static final String EXTERNAL_ID = "EXTERNAL_ID";
	private static final String UUID = "UUID";
	private static final String SAMPLE_NAME = "SAMPLE_NAME";
	private static final String ANONYMIZED_NAME = "ANONYMIZED_NAME";
	private static final String INDIVIDUAL_NAME = "INDIVIDUAL_NAME";
	private static final String SCIENTIFIC_NAME = "SCIENTIFIC_NAME";
	private static final String TAXON_ID = "TAXON_ID";
	private static final String SAMPLE_ATTRIBUTE = "SAMPLE_ATTRIBUTE";
	private static final String SAMPLE_ATTRIBUTES = "SAMPLE_ATTRIBUTES";
	private static final String DESCRIPTION = "DESCRIPTION";
	private static final String TITLE = "TITLE";
	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private TaxonomyService taxonomyService;

	@Override
	public Sample convert(final Element root) {
		final SortedSet<Attribute> attributes = new TreeSet<>();
		final SortedSet<Relationship> relationships = new TreeSet<>();
		final SortedSet<ExternalReference> externalReferences = new TreeSet<>();
		String name = null;
		String accession = null;

		// ENA Specific fields

		// ENA name - BSD-1741 - Requirement#1 Map name (top-attribute) in BioSamples to
		// alias (top-attribute) in ENA XML
		final String primaryId = XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS, PRIMARY_ID).text();

		if (XmlPathBuilder.of(root).path(SAMPLE).attributeExists(ALIAS)) {
			name = XmlPathBuilder.of(root).path(SAMPLE).attribute(ALIAS).trim();
		} else {
			// if and only if alias is not present, then name would be equal to primaryId
			name = primaryId;
		}

		log.trace("Converting ENA sample with PRIMARY_ID as " + primaryId);

		// ENA sra accession
		if (XmlPathBuilder.of(root).path(SAMPLE).attributeExists("accession")) {
			String sraAccession = XmlPathBuilder.of(root).path(SAMPLE).attribute("accession").trim();
			attributes.add(Attribute.build(ENA_SRA_ACCESSION, sraAccession));
		}

		// ENA broker name
		if (XmlPathBuilder.of(root).path(SAMPLE).attributeExists("broker_name")) {
			String brokerName = XmlPathBuilder.of(root).path(SAMPLE).attribute("broker_name").trim();
			attributes.add(Attribute.build(ENA_BROKER_NAME, brokerName));
		}

		// ENA center name
		if (XmlPathBuilder.of(root).path(SAMPLE).attributeExists("center_name")) {
			String centerName = XmlPathBuilder.of(root).path(SAMPLE).attribute("center_name").trim();
			attributes.add(Attribute.build(INSDC_CENTER_NAME, centerName));
		}

		// ENA center alias
		if (XmlPathBuilder.of(root).path(SAMPLE).attributeExists("center_alias")) {
			String centerAlias = XmlPathBuilder.of(root).path(SAMPLE).attribute("center_alias").trim();
			attributes.add(Attribute.build(INSDC_CENTER_ALIAS, centerAlias));
		}

		// ENA title
		String title = "";
		if (XmlPathBuilder.of(root).path(SAMPLE, TITLE).exists()
				&& XmlPathBuilder.of(root).path(SAMPLE, TITLE).text().trim().length() > 0) {
			title = XmlPathBuilder.of(root).path(SAMPLE, TITLE).text().trim();
		} else if (XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, SCIENTIFIC_NAME).exists()
				&& XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, SCIENTIFIC_NAME).text().trim().length() > 0) {
			title = XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, SCIENTIFIC_NAME).text().trim();
		}
		attributes.add(Attribute.build(ENA_TITLE, title));

		// ENA description
		if (XmlPathBuilder.of(root).path(SAMPLE, DESCRIPTION).exists()
				&& XmlPathBuilder.of(root).path(SAMPLE, DESCRIPTION).text().trim().length() > 0) {
			String description = XmlPathBuilder.of(root).path(SAMPLE, DESCRIPTION).text().trim();
			attributes.add(Attribute.build(ENA_DESCRIPTION, description));
		}

		// put various other fields in as synonyms
		Set<String> synonyms = new HashSet<>();
		if (XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS, SUBMITTER_ID).exists()) {
			String synonym = XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS, SUBMITTER_ID).text();
			synonyms.add(synonym);
		}
		if (XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS, EXTERNAL_ID).exists()) {
			for (Element e : XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS).elements(EXTERNAL_ID)) {
				if ("BioSample".equals(e.attributeValue("namespace"))) {
					accession = XmlPathBuilder.of(e).text();
				} else {
					synonyms.add(XmlPathBuilder.of(e).text());
				}
			}
		}
		if (XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS, UUID).exists()) {
			for (Element e : XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS).elements(UUID)) {
				synonyms.add(e.getTextTrim());
			}
		}
		if (XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, ANONYMIZED_NAME).exists()) {
			synonyms.add(XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, ANONYMIZED_NAME).text().trim());
			// attributes.add(Attribute.build("anonymized name",
			// XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, ANONYMIZED_NAME).text()));
		}
		if (XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, INDIVIDUAL_NAME).exists()) {
			synonyms.add(XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, INDIVIDUAL_NAME).text().trim());
			// attributes.add(Attribute.build("individual name",
			// XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, INDIVIDUAL_NAME).text()));
		}
		for (String synonym : synonyms) {
			if (!synonym.equals(name) && !synonym.equals(accession)) {
				attributes.add(Attribute.build("synonym", synonym));
			}
		}

		// Do the organism attribute
		int organismTaxId = Integer.parseInt(XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, TAXON_ID).text());
		String organismUri = taxonomyService.getUriForTaxonId(organismTaxId);
		String organismName = "" + organismTaxId;
		if (XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, SCIENTIFIC_NAME).exists()) {
			organismName = XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, SCIENTIFIC_NAME).text();
		}
		// ideally this should be lowercase, but backwards compatibilty...
		attributes.add(Attribute.build("Organism", organismName, organismUri, null));

		if (XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_ATTRIBUTES).exists()) {
			for (Element e : XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_ATTRIBUTES).elements(SAMPLE_ATTRIBUTE)) {

				String tag = null;
				if (XmlPathBuilder.of(e).path("TAG").exists()
						&& XmlPathBuilder.of(e).path("TAG").text().trim().length() > 0) {
					tag = XmlPathBuilder.of(e).path("TAG").text().trim();
				}
				String value = null;
				if (XmlPathBuilder.of(e).path("VALUE").exists()
						&& XmlPathBuilder.of(e).path("VALUE").text().trim().length() > 0) {
					value = XmlPathBuilder.of(e).path("VALUE").text().trim();
				}
				String unit = null;
				if (XmlPathBuilder.of(e).path("UNITS").exists()
						&& XmlPathBuilder.of(e).path("UNITS").text().trim().length() > 0) {
					unit = XmlPathBuilder.of(e).path("UNITS").text().trim();
				}

				// skip attributes prefixed with ENA
				if (tag != null && tag.startsWith("ENA-")) {
					continue;
				}

				// TODO handle relationships

				if (tag != null) {
					if (value != null) {
						attributes.add(Attribute.build(tag, value, Collections.emptyList(), unit));
					} else {
						// no value supplied
						attributes.add(Attribute.build(tag, "unknown", Collections.emptyList(), unit));
					}
				}

				// BSD-1742 Requirement#2 characteristics/alias in BioSamples should always map to SAMPLE_ATTRIBUTE/alias in ENA XML 
				// Although the sample attribute mappings will already set the alias attribute. This is just to be double sure. 
				if(tag != null && tag.equalsIgnoreCase(ALIAS)) {
					attributes.add(Attribute.build(tag, value));
				}
			}
		}

		if (XmlPathBuilder.of(root).path(SAMPLE, "SAMPLE_LINKS", "URI_LINK").exists()) {
			for (Element e : XmlPathBuilder.of(root).path(SAMPLE, "SAMPLE_LINKS").elements("URI_LINK")) {
				String key = XmlPathBuilder.of(e).attribute("LABEL");
				String value = XmlPathBuilder.of(e).attribute("URL");
				attributes.add(Attribute.build(key, value));
			}
		}

		return new Sample.Builder(name, accession).withRelease(Instant.now()).withUpdate(Instant.now())
				.withAttributes(attributes).withRelationships(relationships).withExternalReferences(externalReferences)
				.build();
	}

}