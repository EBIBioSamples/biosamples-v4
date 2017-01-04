package uk.ac.ebi.biosamples.solr.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.solr.core.mapping.Dynamic;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import uk.ac.ebi.biosamples.models.Attribute;
import uk.ac.ebi.biosamples.models.CustomLocalDateTimeDeserializer;
import uk.ac.ebi.biosamples.models.CustomLocalDateTimeSerializer;
import uk.ac.ebi.biosamples.models.Relationship;


@SolrDocument(solrCoreName = "samples")
public class SolrSample {

	@Id
	@Indexed(name="accession_s", required=true)
	protected String accession;
	@Indexed(name="name_s", required=true)
	protected String name; 

	/**
	 * Store the release date as a string so that it can be used easily by solr
	 * Use a TrieDate type for better range query performance
	 */
	@Indexed(name="release_dt", required=true, type="date")
	//@JsonSerialize(using = CustomLocalDateTimeSolrSerializer.class)
	protected String release;
	/**
	 * Store the update date as a string so that it can be used easily by solr
	 * Use a TrieDate type for better range query performance
	 */
	@Indexed(name="update_dt", required=true, type="date")
	//@JsonSerialize(using = CustomLocalDateTimeSolrSerializer.class)
	protected String update;

	@Indexed(name="*_av_ss")
	@Dynamic
	protected Map<String, List<String>> attributeValues;

	@Indexed(name="*_ai_ss", copyTo={"ontologyiri_ss"})
	@Dynamic
	protected Map<String, List<String>> attributeIris;

	@Indexed(name="*_au_ss")
	@Dynamic
	protected Map<String, List<String>> attributeUnits;
	
	/**
	 * This field shouldn't be populated directly, instead Solr will copy 
	 * all the ontology terms from the attributes into it.
	 */
	@Indexed(name="ontologyiri_ss")
	protected List<String> ontologyIris;
	
	public SolrSample(){}
	
	static public SolrSample build(String name, String accession, LocalDateTime release, LocalDateTime update, 
			Set<Attribute> attributes, Set<Relationship> relationships){
		SolrSample sample = new SolrSample();
		sample.accession = accession;
		sample.name = name;
		sample.release =  DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss'Z'").format(release);
		sample.update = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss'Z'").format(update);
		if (attributes != null) {
			sample.attributeValues = new HashMap<>();
			sample.attributeIris = new HashMap<>();
			sample.attributeUnits = new HashMap<>();
			for (Attribute attr : attributes) {
				if (!sample.attributeValues.containsKey(attr.getKey())) {
					sample.attributeValues.put(attr.getKey(), new ArrayList<>());
				}
				sample.attributeValues.get(attr.getKey()).add(attr.getValue());

				if (!sample.attributeIris.containsKey(attr.getKey())) {
					sample.attributeIris.put(attr.getKey(), new ArrayList<>());
				}
				sample.attributeIris.get(attr.getKey()).add(attr.getIri());

				if (!sample.attributeUnits.containsKey(attr.getKey())) {
					sample.attributeUnits.put(attr.getKey(), new ArrayList<>());
				}
				sample.attributeUnits.get(attr.getKey()).add(attr.getUnit());
			}
		}
		return sample;
	}
}
