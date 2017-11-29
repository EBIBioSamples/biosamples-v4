package uk.ac.ebi.biosamples.solr.service;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.ExternalReferenceService;
import uk.ac.ebi.biosamples.service.SampleRelationshipUtils;
import uk.ac.ebi.biosamples.solr.model.SolrSample;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SampleToSolrSampleConverter implements Converter<Sample, SolrSample> {

	
	private final ExternalReferenceService externalReferenceService;
	
	public SampleToSolrSampleConverter(ExternalReferenceService externalReferenceService) {
		this.externalReferenceService = externalReferenceService;
	}
	
	@Override
	public SolrSample convert(Sample sample) {
		Map<String, List<String>> attributeValues = null;
		Map<String, List<String>> attributeIris = null;
		Map<String, List<String>> attributeUnits = null;
		Map<String, List<String>> outgoingRelationships = null;
		Map<String, List<String>> incomingRelationships = null;
		Map<String, List<String>> externalReferencesData = null;
		List<String> keywords = new ArrayList<>();

		if (sample.getCharacteristics() != null && sample.getCharacteristics().size() > 0) {
			attributeValues = new HashMap<>();
			attributeIris = new HashMap<>();
			attributeUnits = new HashMap<>();

			for (Attribute attr : sample.getCharacteristics()) {
				
				String key = attr.getType();
				//key = SolrSampleService.attributeTypeToField(key);
				
				String value = attr.getValue();
				//if its longer than 255 characters, don't add it to solr
				//solr cant index long things well, and its probably not useful for search
				if (value.length() > 255) {
					continue;
				}
				
				if (!attributeValues.containsKey(key)) {
					attributeValues.put(key, new ArrayList<>());
				}
				
				//if there is a unit, add it to the value for search & facet purposes
				if (attr.getUnit() != null) {
					value = value+" ("+attr.getUnit()+")";
				}
				attributeValues.get(key).add(value);

				//technically, this can't differentiate which iris go with which attribute if there
				//are multiple attributes with the same type
				if (!attributeIris.containsKey(key)) {
					attributeIris.put(key, new ArrayList<>());
				}
				if (attr.getIri().size() == 0) {
					attributeIris.get(key).add("");
				} else {
					for (String iri : attr.getIri()) {
						attributeIris.get(key).add(iri);
					}
				}

				if (!attributeUnits.containsKey(key)) {
					attributeUnits.put(key, new ArrayList<>());
				}
				if (attr.getUnit() == null) {
					attributeUnits.get(key).add("");
				} else {
					attributeUnits.get(key).add(attr.getUnit());
				}
			}
		}	
		//turn external reference into additional attributes for facet & filter
		for (ExternalReference externalReference : sample.getExternalReferences()) {
			String externalReferenceNickname = externalReferenceService.getNickname(externalReference);
			String key = "external reference";
			
			if (attributeValues == null) {
				attributeValues = new HashMap<>();
			}			
			if (!attributeValues.containsKey(key)) {
				attributeValues.put(key, new ArrayList<>());
			}
			attributeValues.get(key).add(externalReferenceNickname);

			// Add the external reference data id
			Optional<String> externalReferenceDataId = externalReferenceService.getDataId(externalReference);
			if (externalReferenceDataId.isPresent()) {

				if (externalReferencesData == null) {
					externalReferencesData = new HashMap<>();
				}

				if (!externalReferencesData.containsKey(externalReferenceNickname)) {
					externalReferencesData.put(externalReferenceNickname, new ArrayList<>());
				}

				externalReferencesData.get(externalReferenceNickname).add(externalReferenceDataId.get());

			}


		}


		// Add relationships owned by sample
		SortedSet<Relationship> sampleOutgoingRelationships = SampleRelationshipUtils.getOutgoingRelationships(sample);
		if ( sampleOutgoingRelationships != null && !sampleOutgoingRelationships.isEmpty()) {
			outgoingRelationships = new HashMap<>();
			for (Relationship rel : sampleOutgoingRelationships) {
				outgoingRelationships.computeIfAbsent(rel.getType(), type -> new ArrayList<>()).add(rel.getTarget());
			}
		}

		// Add relationships for which sample is the target
		SortedSet<Relationship> sampleIngoingRelationships = SampleRelationshipUtils.getIncomingRelationships(sample);
		if ( sampleIngoingRelationships != null && !sampleIngoingRelationships.isEmpty()) {
			incomingRelationships = new HashMap<>();
			for (Relationship rel: sampleIngoingRelationships) {
				incomingRelationships.computeIfAbsent(rel.getType(), type -> new ArrayList<>()).add(rel.getSource());
            }
		}


		String releaseSolr = DateTimeFormatter.ISO_INSTANT.format(sample.getRelease());
		String updateSolr = DateTimeFormatter.ISO_INSTANT.format(sample.getUpdate());

		sample.getOrganizations().forEach(org -> {
			keywords.addAll(Arrays.asList(org.getName(), org.getEmail(), org.getRole(), org.getUrl()));
		});

		sample.getContacts().forEach(contact -> {
			keywords.addAll(Arrays.asList(contact.getName(), contact.getAffiliation(), contact.getUrl()));
		});

		sample.getPublications().forEach(pub -> {
			keywords.addAll(Arrays.asList(pub.getDoi(), pub.getDoi()));
		});
		
		return SolrSample.build(sample.getName(), sample.getAccession(), sample.getDomain(), releaseSolr, updateSolr,
				attributeValues, attributeIris, attributeUnits,
				outgoingRelationships, incomingRelationships,
				externalReferencesData, keywords);
	}
	
}
