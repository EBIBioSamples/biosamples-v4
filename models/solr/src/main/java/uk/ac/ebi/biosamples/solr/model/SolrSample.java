package uk.ac.ebi.biosamples.solr.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.solr.core.mapping.Dynamic;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

import uk.ac.ebi.biosamples.solr.service.SolrSampleService;


@SolrDocument(solrCoreName = "samples")
public class SolrSample {

	/**
	 * Use the accession as the primary document identifier
	 */
	@Id
	@Indexed(name="id", required=true)
	protected String accession;
	@Indexed(name="name_s", required=true, copyTo={"autocomplete_ss"})
	protected String name; 

	/**
	 * Store the release date as a string so that it can be used easily by solr
	 * Use a TrieDate type for better range query performance
	 */
	@Indexed(name="release_dt", required=true, type="date")
	protected String release;
	/**
	 * Store the update date as a string so that it can be used easily by solr
	 * Use a TrieDate type for better range query performance
	 */
	@Indexed(name="update_dt", required=true, type="date")
	protected String update;

	@Indexed(name="*_av_ss", copyTo="autocomplete")
	@Dynamic
	protected Map<String, List<String>> attributeValues;

	@Indexed(name="*_ai_ss", copyTo={"ontologyiri_ss",})
	@Dynamic
	protected Map<String, List<String>> attributeIris;

	@Indexed(name="*_au_ss")
	@Dynamic
	protected Map<String, List<String>> attributeUnits;
	
	/**
	 * This field shouldn't be populated directly, instead Solr will copy 
	 * all the ontology terms from the attributes into it.
	 */
	@Indexed(name="ontologyiri_ss", readonly=true)
	protected List<String> ontologyIris;
	
	/**
	 * This field is required to get a list of attribute to use for faceting.
	 * Since faceting does not require it to be stored, it wont be to save space.
	 * 
	 */
	@Indexed(name="attributetypes_ss", copyTo={"autocomplete_ss",})
	protected List<String> attributeTypes;
	

	/**
	 * This field is required to use with autocomplete faceting.
	 * Since faceting does not require it to be stored, it wont be to save space
	 */
	@Indexed(name="autocomplete_ss", readonly=true)
	protected List<String> autocompleteTerms;
	
	public SolrSample(){}
	
	
	public String getAccession() {
		return accession;
	}


	public String getName() {
		return name;
	}


	public String getRelease() {
		return release;
	}


	public String getUpdate() {
		return update;
	}


	public Map<String, List<String>> getAttributeValues() {
		return attributeValues;
	}


	public Map<String, List<String>> getAttributeIris() {
		return attributeIris;
	}


	public Map<String, List<String>> getAttributeUnits() {
		return attributeUnits;
	}


	public List<String> getOntologyIris() {
		return ontologyIris;
	}
	

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("Sample(");
    	sb.append(name);
    	sb.append(",");
    	sb.append(accession);
    	sb.append(",");
    	sb.append(release);
    	sb.append(",");
    	sb.append(update);
    	sb.append(",");
    	sb.append(attributeValues);
    	sb.append(",");
    	sb.append(attributeIris);
    	sb.append(",");
    	sb.append(attributeUnits);
    	sb.append(")");
    	return sb.toString();
    }


	/**
	 * Avoid using this directly, use the SolrSampleToSampleConverter or SampleToSolrSampleConverter instead
	 * 
	 * @param name
	 * @param accession
	 * @param release
	 * @param update
	 * @param attributes
	 * @param relationships
	 * @return
	 */
	public static SolrSample build(String name, String accession, String release, String update, 
			Map<String, List<String>> attributeValues, Map<String, List<String>> attributeIris, Map<String, List<String>> attributeUnits) {
		SolrSample sample = new SolrSample();
		sample.accession = accession;
		sample.name = name;
		sample.release =  release;
		sample.update = update;
		sample.attributeValues = attributeValues;
		sample.attributeIris = attributeIris;
		sample.attributeUnits = attributeUnits;
		//TODO handle relationships too
		//but how to do inverse?
		//TODO handle external references
		//TODO validate maps
		sample.attributeTypes = null;
		if (attributeValues != null && attributeValues.keySet().size() > 0) {
			List<String> attributeTypes = new ArrayList<>();
			for (String attributeType : attributeValues.keySet()) {
				String field = SolrSampleService.attributeTypeToField(attributeType);
				attributeTypes.add(field);
			}
			Collections.sort(attributeTypes);
			sample.attributeTypes = attributeTypes;
		}
		return sample;
	}
}
