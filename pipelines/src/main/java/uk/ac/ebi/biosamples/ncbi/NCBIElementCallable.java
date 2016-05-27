package uk.ac.ebi.biosamples.ncbi;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.biosamples.models.SimpleSample;
import uk.ac.ebi.biosamples.utils.XMLUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NCBIElementCallable implements Callable<Void> {
	
	private Logger log = LoggerFactory.getLogger(getClass());

	private final Element sampleElem;

	@Autowired
	private XMLUtils xmlUtils;
	
	@Autowired
	private RestTemplate restTemplate;

	private Map<String, Set<String>> keyValues = new HashMap<>();
	// TODO ontology terms? taxonomy?
	// TODO units?

	public NCBIElementCallable(Element sampleElem) {
		this.sampleElem = sampleElem;
	}

	private void addAttribute(String type, String value) {
		type = type.trim();
		value = value.trim();
		// TODO handle odd characters
		if (!keyValues.containsKey(type)) {
			keyValues.put(type, new HashSet<>());
		}
		keyValues.get(type).add(value);
	}

	@Override
	public Void call() throws Exception {

		String accession = sampleElem.attributeValue("accession");

		log.trace("Element callable starting for "+accession);
		
		// TODO compare to last version of XML?

		// convert it to our model

		Element description = xmlUtils.getChildByName(sampleElem, "Description");


		String name = xmlUtils.getChildByName(description, "Title").getTextTrim();
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

		for (Element idElem : xmlUtils.getChildrenByName(xmlUtils.getChildByName(sampleElem, "Ids"), "Id")) {
			String id = idElem.getTextTrim();
			if (!accession.equals(id) && !name.equals(id)) {
				addAttribute("synonym", id);
			}
		}

		Element descriptionCommment = xmlUtils.getChildByName(description, "Comment");
		if (descriptionCommment != null) {
			Element descriptionParagraph = xmlUtils.getChildByName(description, "Paragraph");
			if (descriptionParagraph != null) {
				String secondaryDescription = descriptionParagraph.getTextTrim();
				if (!name.equals(secondaryDescription)) {
					addAttribute("description", secondaryDescription);
				}
			}
		}

		// handle the organism
		Element organismElement = xmlUtils.getChildByName(description, "Organism");
		if (organismElement.attributeValue("taxonomy_id") == null) {
			addAttribute("organism", organismElement.attributeValue("taxonomy_name").trim());
		} else {
			// TODO taxonomy reference
			addAttribute("organism", organismElement.attributeValue("taxonomy_name").trim());
		}

		// handle attributes
		for (Element attrElem : xmlUtils.getChildrenByName(xmlUtils.getChildByName(sampleElem, "Attributes"),
				"Attribute")) {
			String type = attrElem.attributeValue("display_name");
			if (type == null || type.length() == 0) {
				type = attrElem.attributeValue("attribute_name");
			}
			String value = attrElem.getTextTrim();
			addAttribute(type, value);
		}

		// handle model and packages
		for (Element modelElem : xmlUtils.getChildrenByName(xmlUtils.getChildByName(sampleElem, "Models"), "Model")) {
			addAttribute("model", modelElem.getTextTrim());
		}
		addAttribute("package", xmlUtils.getChildByName(sampleElem, "Package").getTextTrim());

		//handle dates
		LocalDate updateDate = null;
		updateDate = LocalDate.parse(sampleElem.attributeValue("last_update"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		LocalDate releaseDate = null;
		releaseDate = LocalDate.parse(sampleElem.attributeValue("publication_date"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		
		LocalDate latestDate = updateDate;
		if (releaseDate.isAfter(latestDate)) {
			latestDate = releaseDate;
		}
		
		SimpleSample sample = SimpleSample.createFrom(name, accession, updateDate, releaseDate, keyValues, new HashMap<>(), new HashMap<>());
		
		log.info("POSTing "+accession);
		
		// TODO handle links

		// send it to the subs API
		// TODO maybe this should be a queue?
        restTemplate.postForLocation("http://localhost:8080/samples", sample);
		

		log.trace("Element callable finished");
		
		return null;
	}

}
