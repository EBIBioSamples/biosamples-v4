package uk.ac.ebi.biosamples.ncbi;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.TaxonomyService;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

public class NcbiElementCallable implements Callable<Void> {
	
	private Logger log = LoggerFactory.getLogger(getClass());

	private final Element sampleElem;

	private final String domain;
	
	private final TaxonomyService taxonomyService;
	
	private final BioSamplesClient bioSamplesClient;

	public NcbiElementCallable(TaxonomyService taxonomyService, BioSamplesClient bioSamplesClient, Element sampleElem, String domain) {
		this.taxonomyService = taxonomyService;
		this.bioSamplesClient = bioSamplesClient;
		this.sampleElem = sampleElem;
		this.domain = domain;
	}

	@Override
	public Void call() throws Exception {

		String accession = sampleElem.attributeValue("accession");

		log.trace("Element callable starting for "+accession);
		
		// TODO compare to last version of XML?
		// convert it to our model
		
		String name = XmlPathBuilder.of(sampleElem).path("Description", "Title").text();
		// if the name is double quotes, strip them
		if (name.startsWith("\"")) {
			name = name.substring(1, name.length()).trim();
		}
		if (name.endsWith("\"")) {
			name = name.substring(0, name.length()-1).trim();
		}
		// if the name is blank, force it
		if (name.trim().length() == 0) {
			name = accession;
		}
		
		SortedSet<Attribute> attrs = new TreeSet<>();
		SortedSet<Relationship> rels = new TreeSet<>();

		for (Element idElem : XmlPathBuilder.of(sampleElem).path("Ids").elements("Id")) {
			String id = idElem.getTextTrim();
			if (!accession.equals(id) && !name.equals(id)) {
				attrs.add(Attribute.build("synonym",  id));
			}
		}

		
		if (XmlPathBuilder.of(sampleElem).path("Description", "Comment", "Paragraph").exists()) {
			String key = "description";
			String value = XmlPathBuilder.of(sampleElem).path("Description", "Comment", "Paragraph").text().trim();
			/*
			if (value.length() > 255) {
				log.warn("Truncating attribute "+key+" for length on "+accession);
				value = value.substring(0, 252)+"...";
			}
			*/
			attrs.add(Attribute.build(key, value));
		}

		// handle the organism		
		String organismIri = null;
		String organismValue = null;
		if (XmlPathBuilder.of(sampleElem).path("Description", "Organism").attributeExists("taxonomy_id")) {
			int taxonId = Integer.parseInt(XmlPathBuilder.of(sampleElem).path("Description", "Organism").attribute("taxonomy_id"));
			organismIri = taxonomyService.getUriForTaxonId(taxonId);
		}
		if (XmlPathBuilder.of(sampleElem).path("Description", "Organism").attributeExists("taxonomy_name")) {
			organismValue = XmlPathBuilder.of(sampleElem).path("Description", "Organism").attribute("taxonomy_name");
		}
		
		if (organismValue != null) {
			attrs.add(Attribute.build("organism", organismValue, organismIri,  null));			
		}
		

		// handle attributes
		for (Element attrElem : XmlPathBuilder.of(sampleElem).path("Attributes").elements("Attribute")) {
			String key = attrElem.attributeValue("display_name");
			if (key == null || key.length() == 0) {
				key = attrElem.attributeValue("attribute_name");
			}
			String value = attrElem.getTextTrim();
			/*
			if (value.length() > 255) {
				log.warn("Truncating attribute "+key+" for length on "+accession);
				value = value.substring(0, 252)+"...";
			}
			*/
			//value is a sample accession, assume its a relationship
			if (value.matches("SAM[END]A?[0-9]+")) {
				//if its a self-relationship, then don't add it
				//otherwise add it
				if (!value.equals(accession)) {
					rels.add(Relationship.build(accession, key, value));
				}				
			} else {
				//its an attribute
				attrs.add(Attribute.build(key, value));
			}
		}

		// handle model and packages
//disabled for the moment, do they really add anything? faulcon@2017/01/25
//yes, ENA want them. But we can name them better. faulcon@2018/02/14
		for (Element modelElem : XmlPathBuilder.of(sampleElem).path("Models").elements("Model")) {
			attrs.add(Attribute.build("NCBI submission model", modelElem.getTextTrim()));
		}
		attrs.add(Attribute.build("NCBI submission package", XmlPathBuilder.of(sampleElem).path("Package").text()));

		//handle dates
		Instant lastUpdate = Instant.parse(sampleElem.attributeValue("last_update")+"Z");
		Instant publicationDate = Instant.parse(sampleElem.attributeValue("publication_date")+"Z");
		
		Instant latestDate = lastUpdate;
		if (publicationDate.isAfter(latestDate)) {
			latestDate = publicationDate;
		}
		
		//add some INSDC things for standardisation with ENA import
		attrs.add(Attribute.build("INSDC first public", 
			DateTimeFormatter.ISO_INSTANT.format(lastUpdate)));
		attrs.add(Attribute.build("INSDC last update", 
			DateTimeFormatter.ISO_INSTANT.format(publicationDate)));
		
		Sample sample = Sample.build(name, accession, domain, publicationDate, lastUpdate, attrs, rels, null);
		
		//now pass it along to the actual submission process
		bioSamplesClient.persistSampleResource(sample);

		log.trace("Element callable finished");
		
		return null;
	}

}
