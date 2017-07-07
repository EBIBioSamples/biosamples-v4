package uk.ac.ebi.biosamples.solr.model;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
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
	@Indexed(name="update_dt", required=true, type="date") //TODO why type=date ?
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
	@Indexed(name="ontologyiri_ss")
	protected List<String> ontologyIris;
	
	/**
	 * This field is required to get a list of attribute to use for faceting.
	 * Since faceting does not require it to be stored, it wont be to save space.
	 * 
	 */
	@Indexed(name="attributetypes_ss", copyTo={"autocomplete_ss",})
	protected List<String> attributeTypes;
	//TODO consider renaming as used only for faceting
	

	/**
	 * This field is required to use with autocomplete faceting.
	 * Since faceting does not require it to be stored, it wont be to save space
	 */
	@Indexed(name="autocomplete_ss")
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
	 */
	public static SolrSample build(String name, String accession, String release, String update, 
			Map<String, List<String>> attributeValues, Map<String, List<String>> attributeIris, 
			Map<String, List<String>> attributeUnits) {
		SolrSample sample = new SolrSample();
		sample.accession = accession;
		sample.name = name;
		sample.release =  release;
		sample.update = update;
		
		
		sample.attributeValues = new HashMap<>();
		sample.attributeIris = new HashMap<>();
		sample.attributeUnits = new HashMap<>();

		if (attributeValues != null) {
			for (String key : attributeValues.keySet()) {
				//solr only allows alphanumeric field types
				String base64Key;
				try {
					base64Key = Base64.getEncoder().encodeToString(key.getBytes("UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
				String safeKey = base64Key.replaceAll("=", "_");
				sample.attributeValues.put(safeKey, attributeValues.get(key));
			}
		}

		if (attributeIris != null) {
			for (String key : attributeIris.keySet()) {
				//solr only allows alphanumeric field types
				String base64Key;
				try {
					base64Key = Base64.getEncoder().encodeToString(key.getBytes("UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
				String safeKey = base64Key.replaceAll("=", "_");
				sample.attributeIris.put(safeKey, attributeIris.get(key));
			}
		}

		if (attributeUnits != null) {
			for (String key : attributeUnits.keySet()) {
				//solr only allows alphanumeric field types
				String base64Key;
				try {
					base64Key = Base64.getEncoder().encodeToString(key.getBytes("UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
				String safeKey = base64Key.replaceAll("=", "_");
				sample.attributeUnits.put(safeKey, attributeUnits.get(key));
			}
		}
		
		
		
		
		//TODO handle relationships too
		//but how to do inverse?
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
		
		//copy into the other fields
		//this should be done in a copyfield but that doesn't work for some reason?
		sample.autocompleteTerms = new ArrayList<>();
		if (attributeValues != null) {
			sample.autocompleteTerms.addAll(attributeValues.keySet());
			for (List<String> values : attributeValues.values()) {
				sample.autocompleteTerms.addAll(values);
			}
		}
		return sample;
	}
}
