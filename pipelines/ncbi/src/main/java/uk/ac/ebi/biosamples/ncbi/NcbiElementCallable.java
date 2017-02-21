package uk.ac.ebi.biosamples.ncbi;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.HateoasUtils;
import uk.ac.ebi.biosamples.utils.SubmissionService;
import uk.ac.ebi.biosamples.utils.TaxonomyService;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;
import uk.ac.ebi.biosamples.utils.XmlUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NcbiElementCallable implements Callable<Void> {
	
	private Logger log = LoggerFactory.getLogger(getClass());

	private final Element sampleElem;
	
	@Autowired
	private TaxonomyService taxonomyService;
	
	@Autowired
	private SubmissionService submissionService;

	public NcbiElementCallable(Element sampleElem) {
		this.sampleElem = sampleElem;
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
				attrs.add(Attribute.build("synonym",  id,  null,  null));
			}
		}

		
		if (XmlPathBuilder.of(sampleElem).path("Description", "Comment", "Paragraph").exists()) {
			attrs.add(Attribute.build("description", XmlPathBuilder.of(sampleElem).path("Description", "Comment", "Paragraph").text(),  null,  null));
		}

		// handle the organism		
		URI organismIri = null;
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
			//value is a sample accession, assume its a relationship
			if (value.matches("SAM[END]A?[0-9]+")) {
				//if its a self-relationship, then dont add it
				if (!value.equals(accession)) {
					rels.add(Relationship.build(key, value, accession));
				}
			} else {
				//its an attribute
				attrs.add(Attribute.build(key, value, null, null));
			}
		}

		// handle model and packages
//disabled for the moment, do they really add anything? faulcon@2017/01/25
//		for (Element modelElem : XmlPathBuilder.of(sampleElem).path("Models").elements("Model")) {
//			attrs.add(Attribute.build("model", modelElem.getTextTrim(), null, null));
//		}
//		attrs.add(Attribute.build("package", XmlPathBuilder.of(sampleElem).path("Package").text(), null, null));

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
		submissionService.submit(sample);

		log.trace("Element callable finished");
		
		return null;
	}

}
