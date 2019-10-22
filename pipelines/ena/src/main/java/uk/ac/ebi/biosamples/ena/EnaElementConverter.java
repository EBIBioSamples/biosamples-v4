package uk.ac.ebi.biosamples.ena;

import java.time.Instant;
import java.util.Collections;
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
	private static final String ORGANISM = "Organism";
	private static final String DESCRIPTION_SAMPLE_ATTRIBUTE = "attribute";
	private static final String DESCRIPTION_CORE = "core";
	// Fields required by ENA content - some are for JSON building and some for
	// equality checks with ENA XML
	private static final String UUID_JSON = "uuid";
	private static final String INDIVIDUAL_NAME_JSON = "individual_name";
	private static final String ANONYMIZED_NAME_JSON = "anonymized_name";
	private static final String BIOSAMPLE = "BioSample";
	private static final String NAMESPACE = "namespace";
	private static final String NAMESPACE_TAG = "Namespace:";
	private static final String SUBMITTER_ID_JSON = "Submitter Id";
	private static final String EXTERNAL_ID_JSON = "External Id";
	private static final String ALIAS = "alias";
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
		if (XmlPathBuilder.of(root).path(SAMPLE, TITLE).exists() && XmlPathBuilder.of(root).path(SAMPLE, TITLE).text().trim().length() > 0) {
			title = XmlPathBuilder.of(root).path(SAMPLE, TITLE).text().trim();
		} else if (XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, SCIENTIFIC_NAME).exists()
				&& XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, SCIENTIFIC_NAME).text().trim().length() > 0) {
			title = XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, SCIENTIFIC_NAME).text().trim();
		}
		attributes.add(Attribute.build(ENA_TITLE, title));

		// ENA description - BSD-1744 - Deal with multiple descriptions in ENA XML
		final XmlPathBuilder descriptionPathBuilder = XmlPathBuilder.of(root).path(SAMPLE, DESCRIPTION);

		if (descriptionPathBuilder.exists() && descriptionPathBuilder.text().trim().length() > 0) {
			final String description = descriptionPathBuilder.text().trim();

			attributes.add(Attribute.build(ENA_DESCRIPTION, description, DESCRIPTION_CORE, Collections.emptyList(), null));
		}

		// ENA SUBMITTER_ID - BSD-1743 - Un-tag core attributes and sample attributes from synonyms
		final XmlPathBuilder submitterIdPathBuilder = XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS, SUBMITTER_ID);

		if (submitterIdPathBuilder.exists()) {
			attributes.add(Attribute.build(SUBMITTER_ID_JSON, submitterIdPathBuilder.text(), NAMESPACE_TAG + submitterIdPathBuilder.attribute(NAMESPACE),
					Collections.emptyList(), null));
		}

		// ENA EXTERNAL_ID - BSD-1743 - Un-tag core attributes and sample attributes from synonyms
		final XmlPathBuilder externalIdPathBuilder = XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS, EXTERNAL_ID);

		if (externalIdPathBuilder.exists()) {
			for (final Element element : XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS).elements(EXTERNAL_ID)) {
				final String externalIdElement = XmlPathBuilder.of(element).text();

				attributes.add(Attribute.build(EXTERNAL_ID_JSON, externalIdElement, NAMESPACE_TAG + externalIdPathBuilder.attribute(NAMESPACE),
						Collections.emptyList(), null));

				if (BIOSAMPLE.equals(element.attributeValue(NAMESPACE))) {
					accession = externalIdElement;
				}
			}
		}

		// ENA ANONYMIZED_NAME - BSD-1743 - Un-tag core attributes and sample attributes from synonyms
		final XmlPathBuilder anonymizedNamePathBuilder = XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, ANONYMIZED_NAME);

		if (anonymizedNamePathBuilder.exists()) {
			attributes.add(Attribute.build(ANONYMIZED_NAME_JSON, anonymizedNamePathBuilder.text()));
		}

		// ENA INDIVIDUAL_NAME - BSD-1743 - Un-tag core attributes and sample attributes from synonyms
		final XmlPathBuilder individualNamePathBuider = XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, INDIVIDUAL_NAME);

		if (individualNamePathBuider.exists()) {
			attributes.add(Attribute.build(INDIVIDUAL_NAME_JSON, individualNamePathBuider.text()));
		}

		// ENA UUID - BSD-1743 - Un-tag core attributes and sample attributes from synonyms
		if (XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS, UUID).exists()) {
			for (Element element : XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS).elements(UUID)) {
				attributes.add(Attribute.build(UUID_JSON, element.getTextTrim()));
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
		attributes.add(Attribute.build(ORGANISM, organismName, organismUri, null));

		if (XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_ATTRIBUTES).exists()) {
			for (Element e : XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_ATTRIBUTES).elements(SAMPLE_ATTRIBUTE)) {
				String tag = null;

				if (XmlPathBuilder.of(e).path("TAG").exists() && XmlPathBuilder.of(e).path("TAG").text().trim().length() > 0) {
					tag = XmlPathBuilder.of(e).path("TAG").text().trim();
				}

				String value = null;

				if (XmlPathBuilder.of(e).path("VALUE").exists() && XmlPathBuilder.of(e).path("VALUE").text().trim().length() > 0) {
					value = XmlPathBuilder.of(e).path("VALUE").text().trim();
				}

				String unit = null;

				if (XmlPathBuilder.of(e).path("UNITS").exists() && XmlPathBuilder.of(e).path("UNITS").text().trim().length() > 0) {
					unit = XmlPathBuilder.of(e).path("UNITS").text().trim();
				}

				// TODO handle relationships

				// BSD-1744 - Deal with multiple descriptions in ENA XML
				if(tag != null && tag.equalsIgnoreCase(ENA_DESCRIPTION)) {
					attributes.add(Attribute.build(ENA_DESCRIPTION, value, DESCRIPTION_SAMPLE_ATTRIBUTE, Collections.emptyList(), null));
					continue;
				}

				if (tag != null) {
					if (value != null) {
						attributes.add(Attribute.build(tag, value, null, Collections.emptyList(), unit));
					} else {
						// no value supplied
						attributes.add(Attribute.build(tag, "", null, Collections.emptyList(), unit));
					}
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

		return new Sample.Builder(name, accession).withRelease(Instant.now()).withUpdate(Instant.now()).withAttributes(attributes)
				.withRelationships(relationships).withExternalReferences(externalReferences).build();
	}

}