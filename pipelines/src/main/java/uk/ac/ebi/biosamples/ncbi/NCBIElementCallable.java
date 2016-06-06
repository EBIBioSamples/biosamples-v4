package uk.ac.ebi.biosamples.ncbi;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
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
	

	private static URI uri; 
	static {
		try {
			uri = new URI("http://localhost:8080/samples");
		} catch (URISyntaxException e) {
			//should never happen
			throw new RuntimeException(e);
		}
	}

	private Map<String, Set<String>> keyValues = new HashMap<>();
	// TODO ontology terms? taxonomy?
	// TODO units?

	private Map<String, Set<String>> relationships = new HashMap<>();

	public NCBIElementCallable(Element sampleElem) {
		this.sampleElem = sampleElem;
	}

	private void addAttribute(String key, String value) {
		key = key.trim();
		value = value.trim();
		// TODO handle odd characters
		if (!keyValues.containsKey(key)) {
			keyValues.put(key, new HashSet<>());
		}
		keyValues.get(key).add(value);
	}

	private void addRelationship(String type, String value) {
		type = type.trim();
		value = value.trim();
		if (!relationships.containsKey(type)) {
			relationships.put(type, new HashSet<>());
		}
		relationships.get(type).add(value);
	}

	private void submit(SimpleSample sample) {
		// send it to the subs API
		log.info("GETing "+sample.getAccession());

		//work out if its a put or a post
		//do a get for that
		boolean exists = false;
		try {			
			//wrap the response in a hateoas resource to capture links
			//have to use a parameterizedTypeReference subclass instance to pass the generic information about
			ResponseEntity<Resource<SimpleSample>> getResponse = restTemplate.exchange("http://localhost:8080/repository/mongoSamples/search/findByAccession?accession="+sample.getAccession(),
					HttpMethod.GET,
					null,
					new ParameterizedTypeReference<Resource<SimpleSample>>(){});
			exists = true;
		} catch (HttpStatusCodeException e) {
			if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
				//got a 404, we can handle that by posting instead of putting
				exists = false;
			} else {
				//something else, re-throw it because we can't recover
				log.error("Unable to GET "+sample.getAccession());
				log.error(e.getResponseBodyAsString());
				throw e;
			}
		}
		
		if (exists) {
			log.info("PUTing "+sample.getAccession());
			//was there, so we need to PUT an update
			RequestEntity<SimpleSample> putRequest = RequestEntity.put(uri).body(sample);
			log.error(putRequest.toString());
			ResponseEntity<SimpleSample> putResponse = restTemplate.exchange(putRequest, SimpleSample.class);
			if (!putResponse.getStatusCode().is2xxSuccessful()) {
				log.error(putResponse.toString());
				throw new RuntimeException("Problem PUTing "+sample.getAccession());
			}
		} else {
			log.info("POSTing "+sample.getAccession());
			//not there, so need to POST it
			RequestEntity<SimpleSample> postRequest = RequestEntity.post(uri).body(sample);
			log.error(postRequest.toString());
			ResponseEntity<SimpleSample> postResponse = restTemplate.exchange(postRequest, SimpleSample.class);
			if (!postResponse.getStatusCode().is2xxSuccessful()) {
				throw new RuntimeException("Problem POSTing "+sample.getAccession());
			}
		}
		
        //restTemplate.postForLocation("http://localhost:8080/samples", sample);
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
			Element descriptionParagraph = xmlUtils.getChildByName(descriptionCommment, "Paragraph");
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
			String key = attrElem.attributeValue("display_name");
			if (key == null || key.length() == 0) {
				key = attrElem.attributeValue("attribute_name");
			}
			String value = attrElem.getTextTrim();
			if (value.matches("SAM[END]A?[0-9]+")) {
				//value is a sample accession, assume its a relationship
				addRelationship(key, value);
			} else {
				//its an attribute
				addAttribute(key, value);
			}
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
		
		SimpleSample sample = SimpleSample.createFrom(name, accession, updateDate, releaseDate, keyValues, new HashMap<>(), new HashMap<>(),relationships);
		
		//now pass it along to the actual submission process
		submit(sample);

		log.trace("Element callable finished");
		
		return null;
	}

}
