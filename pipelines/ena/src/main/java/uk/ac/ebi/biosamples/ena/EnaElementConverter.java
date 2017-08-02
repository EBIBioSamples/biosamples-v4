package uk.ac.ebi.biosamples.ena;

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

	private Logger log = LoggerFactory.getLogger(getClass());

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
	
	@Autowired
	private TaxonomyService taxonomyService;
	
	@Autowired
	private EnaService enaService;

	@Override
	public Sample convert(Element root) {

		String name = XmlPathBuilder.of(root).path(SAMPLE, IDENTIFIERS, PRIMARY_ID).text();
		String accession = null;

		SortedSet<Attribute> attributes = new TreeSet<>();
		SortedSet<Relationship> relationships = new TreeSet<>();
		SortedSet<ExternalReference> externalReferences = new TreeSet<>();

		log.trace("Converting " + name);

		//put various other fields in as synonyms
		Set<String> synonyms = new HashSet<>();
		synonyms.add(XmlPathBuilder.of(root).path(SAMPLE).attribute("alias"));
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
			synonyms.add(XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, ANONYMIZED_NAME).text());
			//attributes.add(Attribute.build("anonymized name", XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, ANONYMIZED_NAME).text()));
		}
		if (XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, INDIVIDUAL_NAME).exists()) {
			synonyms.add(XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, INDIVIDUAL_NAME).text());
			//attributes.add(Attribute.build("individual name", XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, INDIVIDUAL_NAME).text()));
		}
		for (String synonym : synonyms) {
			if (!synonym.equals(name) && !synonym.equals(accession)) {
				attributes.add(Attribute.build("synonym", synonym));
			}
		}
		
		//description
		if (XmlPathBuilder.of(root).path(SAMPLE, DESCRIPTION).exists()) {
			String description = XmlPathBuilder.of(root).path(SAMPLE, DESCRIPTION).text();
			attributes.add(Attribute.build("description", description));			
		}
		
        //Do the organism attribute
		int organismTaxId = Integer.parseInt(XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, TAXON_ID).text());
		String organismUri = taxonomyService.getUriForTaxonId(organismTaxId);
		String organismName = ""+organismTaxId;
		if (XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, SCIENTIFIC_NAME).exists()) {
			organismName = XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_NAME, SCIENTIFIC_NAME).text();
		}
		attributes.add(Attribute.build("organism", organismName, organismUri, null));

		if (XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_ATTRIBUTES).exists()) {
			for (Element e : XmlPathBuilder.of(root).path(SAMPLE, SAMPLE_ATTRIBUTES).elements(SAMPLE_ATTRIBUTE)) {

				String tag = null;
				if (XmlPathBuilder.of(e).path("TAG").exists()) {
					tag = XmlPathBuilder.of(e).path("TAG").text();
				}
				String value = null;
				if (XmlPathBuilder.of(e).path("VALUE").exists()) {
					value = XmlPathBuilder.of(e).path("VALUE").text();
				}
				String unit = null;
				if (XmlPathBuilder.of(e).path("UNITS").exists()) {
					unit = XmlPathBuilder.of(e).path("UNITS").text();
				}

				//log.info("Attribute "+tag+" : "+value+" : "+unit);
				
				// skip artificial attributes
				if (tag.startsWith("ENA-")) {
					continue;
				}
				if (tag.startsWith("ArrayExpress-")) {
					continue;
				}
				
				//TODO handle relationships

				if (value != null) {
					attributes.add(Attribute.build(tag, value, null, unit));
				} else {
					// no value supplied
					attributes.add(Attribute.build("unknown", tag, null, unit));
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

	    //external reference
		if (XmlPathBuilder.of(root).path(SAMPLE, "SAMPLE_LINKS", "SAMPLE_LINK").exists()) {
			for (Element e : XmlPathBuilder.of(root).path(SAMPLE, "SAMPLE_LINKS").elements("SAMPLE_LINK")) {
				if (XmlPathBuilder.of(e).path("XREF_LINK", "DB").exists() && XmlPathBuilder.of(e).path("XREF_LINK", "ID").exists()) {
					String db = XmlPathBuilder.of(e).path("XREF_LINK", "DB").text();
					String idGrouped = XmlPathBuilder.of(e).path("XREF_LINK", "ID").text();
					//sometimes ids might be comma separated or a range joined by a dash
					for (String id : enaService.splitIdentifiers(idGrouped)) {
						if ("ENA-EXPERIMENT".equals(db)) {
							//externalReferences.add(ExternalReference.build("https://www.ebi.ac.uk/ena/data/view/"+id));						
						} else if ("ENA-ANALYSIS".equals(db)) {
							//externalReferences.add(ExternalReference.build("https://www.ebi.ac.uk/ena/data/view/"+id));						
						}
						//TODO decide if link to samples, or to experiment+analysis
					}
				}				
			}
		}
		//add external link on the sample
		externalReferences.add(ExternalReference.build("https://www.ebi.ac.uk/ena/data/view/"+accession));
		
		return Sample.build(name, accession, null, null, attributes, relationships, externalReferences);
	}

}