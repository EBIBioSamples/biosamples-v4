package uk.ac.ebi.biosamples.ncbi;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.UriTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.models.Attribute;
import uk.ac.ebi.biosamples.models.Relationship;
import uk.ac.ebi.biosamples.models.Sample;
import uk.ac.ebi.biosamples.utils.HateoasUtils;
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
	private HateoasUtils hateoasUtils;
	
	@Autowired
	private PipelinesProperties pipelinesProperties;
	
	//use RestOperations as the interface implemented by RestTemplate
	//easier to mock for testing
	@Autowired
	private RestOperations restTemplate;
	

	private Map<String, Set<String>> keyValues = new HashMap<>();
	// TODO ontology terms? taxonomy?
	// TODO units?

	private Map<String, Set<String>> relationships = new HashMap<>();

	public NCBIElementCallable(Element sampleElem) {
		this.sampleElem = sampleElem;
	}

	private void submit(Sample sample) {
		// send it to the subs API
		log.info("Checking for existing sample "+sample.getAccession());

		//work out if its a put or a post
		//do a get for that
		URI existingUri = null;
		try {
			
			log.info("Reading URI : "+pipelinesProperties.getBiosampleSubmissionURI());
			
			UriTemplate uriTemplate = hateoasUtils.getHateoasUriTemplate(pipelinesProperties.getBiosampleSubmissionURI(), 
					"mongoSamples", "search", "findByAccession");
			URI uri = uriTemplate.expand(sample.getAccession());
			
			log.info("URI for testing is "+uri);
			
			ResponseEntity<Resources<Resource<Sample>>> response = hateoasUtils.getHateoasResponse(uri,
					new ParameterizedTypeReference<Resources<Resource<Sample>>>(){});
			
			if (response.getStatusCode() == HttpStatus.OK) {
				//check the number of results 
				Collection<Resource<Sample>> resources = response.getBody().getContent();
				if (resources.size() == 0) {
					//this accession has never been seen before, need to POST
					existingUri = null;
				} else {
					//there might be multiple results because we store each version separately
					//this is okay, we can still just PUT to the URI of any of them
					existingUri = URI.create(resources.iterator().next().getLink("self").getHref());
				} 
			} else {
				throw new RuntimeException("Unable to GET "+sample.getAccession());
			}
		} catch (HttpStatusCodeException e) {
			//re-throw it because we can't recover
			log.error("Unable to GET "+sample.getAccession()+" : "+e.getResponseBodyAsString());
			throw e;
		}
		
		if (existingUri != null) {
			log.info("PUTing "+sample.getAccession());
			//was there, so we need to PUT an update
			
			HttpEntity<Sample> requestEntity = new HttpEntity<>(sample);
			ResponseEntity<Resource<Sample>> putResponse = restTemplate.exchange(existingUri,
					HttpMethod.PUT,
					requestEntity,
					new ParameterizedTypeReference<Resource<Sample>>(){});
			
			if (!putResponse.getStatusCode().is2xxSuccessful()) {
				log.error("Unable to PUT "+sample.getAccession()+" : "+putResponse.toString());
				throw new RuntimeException("Problem PUTing "+sample.getAccession());
			}
		} else {
			log.info("POSTing "+sample.getAccession());
			//not there, so need to POST it
			//POST goes to a different URI
			UriTemplate uriTemplate = hateoasUtils.getHateoasUriTemplate(pipelinesProperties.getBiosampleSubmissionURI(), 
					"mongoSamples");
			
			URI urlTarget = uriTemplate.expand();
			
			HttpEntity<Sample> requestEntity = new HttpEntity<>(sample);
			ResponseEntity<Resource<Sample>> postResponse = restTemplate.exchange(urlTarget,
					HttpMethod.POST,
					requestEntity,
					new ParameterizedTypeReference<Resource<Sample>>(){});
			
			if (!postResponse.getStatusCode().is2xxSuccessful()) {
				log.error("Unable to POST "+sample.getAccession()+" : "+postResponse.toString());
				throw new RuntimeException("Problem POSTing "+sample.getAccession());
			}
		}
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
		
		SortedSet<Attribute> attrs = new TreeSet<>();
		SortedSet<Relationship> rels = new TreeSet<>();

		for (Element idElem : xmlUtils.getChildrenByName(xmlUtils.getChildByName(sampleElem, "Ids"), "Id")) {
			String id = idElem.getTextTrim();
			if (!accession.equals(id) && !name.equals(id)) {
				attrs.add(Attribute.build("synonym",  id,  null,  null));
			}
		}

		Element descriptionCommment = xmlUtils.getChildByName(description, "Comment");
		if (descriptionCommment != null) {
			Element descriptionParagraph = xmlUtils.getChildByName(descriptionCommment, "Paragraph");
			if (descriptionParagraph != null) {
				String secondaryDescription = descriptionParagraph.getTextTrim();
				if (!name.equals(secondaryDescription)) {
					attrs.add(Attribute.build("description", secondaryDescription,  null,  null));
				}
			}
		}

		// handle the organism
		Element organismElement = xmlUtils.getChildByName(description, "Organism");
		if (organismElement.attributeValue("taxonomy_id") == null) {
			attrs.add(Attribute.build("organism", organismElement.attributeValue("taxonomy_name").trim(),  null,  null));
		} else {
			// TODO taxonomy reference
			attrs.add(Attribute.build("organism", organismElement.attributeValue("taxonomy_name").trim(),  null,  null));
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
				rels.add(Relationship.build(key, value));
			} else {
				//its an attribute
				attrs.add(Attribute.build(key, value, null, null));
			}
		}

		// handle model and packages
		for (Element modelElem : xmlUtils.getChildrenByName(xmlUtils.getChildByName(sampleElem, "Models"), "Model")) {
			attrs.add(Attribute.build("model", modelElem.getTextTrim(), null, null));
		}
		attrs.add(Attribute.build("package", xmlUtils.getChildByName(sampleElem, "Package").getTextTrim(), null, null));

		//handle dates
		LocalDateTime updateDate = null;
		updateDate = LocalDateTime.parse(sampleElem.attributeValue("last_update"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		LocalDateTime releaseDate = null;
		releaseDate = LocalDateTime.parse(sampleElem.attributeValue("publication_date"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		
		LocalDateTime latestDate = updateDate;
		if (releaseDate.isAfter(latestDate)) {
			latestDate = releaseDate;
		}
		
		//Sample sample = Sample.createFrom(name, accession, updateDate, releaseDate, keyValues, new HashMap<>(), new HashMap<>(),relationships);
		Sample sample = Sample.build(name, accession, releaseDate, updateDate, attrs, rels);
		
		//now pass it along to the actual submission process
		submit(sample);

		log.trace("Element callable finished");
		
		return null;
	}

}
