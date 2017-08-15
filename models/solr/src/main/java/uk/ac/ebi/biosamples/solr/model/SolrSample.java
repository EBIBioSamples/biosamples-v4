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
	 * Relationships for which this sample is the source
	 */
	@Indexed(name="*_or_ss")
	@Dynamic
	protected Map<String, List<String>> outgoingRelationships;




	/**
	 * Relationships for which this sample is the target
	 */
	@Indexed(name="*_ir_ss")
	@Dynamic
	protected Map<String, List<String>> incomingRelationships;

	/**
	 * This field shouldn't be populated directly, instead Solr will copy 
	 * all the ontology terms from the attributes into it.
	 */
	@Indexed(name="ontologyiri_ss")
	protected List<String> ontologyIris;
	
	/**
	 * This field is required to get a list of attribute to use for faceting.
	 * It includes attributes and relationships of the sample
	 * Since faceting does not require it to be stored, it wont be to save space.
	 * 
	 */
	@Indexed(name="facetfields_ss", copyTo={"autocomplete_ss",})
	protected List<String> facetFields;
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


	public Map<String, List<String>> getIncomingRelationships() {
		return incomingRelationships;
	}
	public Map<String, List<String>> getOutgoingRelationships() {
		return outgoingRelationships;
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
    	sb.append(",");
    	sb.append(outgoingRelationships);
    	sb.append(",");
    	sb.append(incomingRelationships);
    	sb.append(")");
    	return sb.toString();
    }


	/**
	 * Avoid using this directly, use the SolrSampleToSampleConverter or SampleToSolrSampleConverter instead
	 * 
	 */
	public static SolrSample build(String name, String accession, String release, String update, 
			Map<String, List<String>> attributeValues, Map<String, List<String>> attributeIris, 
			Map<String, List<String>> attributeUnits, Map<String, List<String>> outgoingRelationships,
            Map<String,List<String>> incomingRelationships) {
		SolrSample sample = new SolrSample();
		sample.accession = accession;
		sample.name = name;
		sample.release =  release;
		sample.update = update;


		sample.attributeValues = new HashMap<>();
		sample.attributeIris = new HashMap<>();
		sample.attributeUnits = new HashMap<>();
		sample.incomingRelationships = new HashMap<>();
		sample.outgoingRelationships = new HashMap<>();

		if (attributeValues != null) {
			for (String key : attributeValues.keySet()) {
				//solr only allows alphanumeric field types
				sample.attributeValues.put(SolrSampleService.valueToSafeField(key), attributeValues.get(key));
			}
		}

		if (attributeIris != null) {
			for (String key : attributeIris.keySet()) {
				//solr only allows alphanumeric field types
				sample.attributeIris.put(SolrSampleService.valueToSafeField(key), attributeIris.get(key));
			}
		}

		if (attributeUnits != null) {
			for (String key : attributeUnits.keySet()) {
				//solr only allows alphanumeric field types
				sample.attributeUnits.put(SolrSampleService.valueToSafeField(key), attributeUnits.get(key));
			}
		}

		if (outgoingRelationships != null) {
            for (String key : outgoingRelationships.keySet()) {
                sample.outgoingRelationships.put(SolrSampleService.valueToSafeField(key), outgoingRelationships.get(key));
            }
		}

		if (incomingRelationships != null) {
		    for (String key: incomingRelationships.keySet()) {
		        sample.incomingRelationships.put(SolrSampleService.valueToSafeField(key), incomingRelationships.get(key));
            }
		}

		//TODO validate maps
		sample.facetFields = null;
		if (attributeValues != null && attributeValues.keySet().size() > 0) {
			List<String> attributeTypes = new ArrayList<>();
			for (String attributeType : attributeValues.keySet()) {
				String field = SolrSampleService.valueToSafeField(attributeType, "_av_ss");
				attributeTypes.add(field);
			}
			Collections.sort(attributeTypes);
			sample.facetFields = attributeTypes;
		}

		if (outgoingRelationships != null && outgoingRelationships.keySet().size() > 0) {
			List<String> outgoingRelationshipTypes = new ArrayList<>();
			for (String key: outgoingRelationships.keySet()) {
                String field = SolrSampleService.valueToSafeField(key, "_or_ss");
				outgoingRelationshipTypes.add(field);
			}

			Collections.sort(outgoingRelationshipTypes);
			if (sample.facetFields != null) {
				sample.facetFields.addAll(outgoingRelationshipTypes);
			}
		}

		if (incomingRelationships != null && incomingRelationships.keySet().size() > 0) {
			List<String> incomingRelationshipTypes = new ArrayList<>();
			for (String key: incomingRelationships.keySet()) {
                String field = SolrSampleService.valueToSafeField(key, "_ir_ss");
				incomingRelationshipTypes.add(field);
			}

			Collections.sort(incomingRelationshipTypes);
			if (sample.facetFields != null) {
				sample.facetFields.addAll(incomingRelationshipTypes);
			}
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
