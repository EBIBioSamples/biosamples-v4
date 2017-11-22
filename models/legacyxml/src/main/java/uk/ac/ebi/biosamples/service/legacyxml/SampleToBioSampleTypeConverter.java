package uk.ac.ebi.biosamples.service.legacyxml;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.legacyxml.BioSampleType;
import uk.ac.ebi.biosamples.model.legacyxml.DatabaseType;
import uk.ac.ebi.biosamples.model.legacyxml.GroupIdsType;
import uk.ac.ebi.biosamples.model.legacyxml.PropertyType;
import uk.ac.ebi.biosamples.model.legacyxml.QualifiedValueType;
import uk.ac.ebi.biosamples.model.legacyxml.TermSourceREFType;
import uk.ac.ebi.biosamples.service.ExternalReferenceService;

@Service
public class SampleToBioSampleTypeConverter implements Converter<Sample, BioSampleType> {

	private final ExternalReferenceService externalReferenceService;
	
	public SampleToBioSampleTypeConverter(ExternalReferenceService externalReferenceService) {
		this.externalReferenceService = externalReferenceService;
	}
	
	@Override
	public BioSampleType convert(Sample source) {
		BioSampleType bioSampleType = new BioSampleType();
		
		bioSampleType.setId(source.getAccession());
		bioSampleType.setSubmissionReleaseDate(DateTimeFormatter.ISO_INSTANT.format(source.getRelease()).replace("Z", "+00:00"));
		bioSampleType.setSubmissionUpdateDate(DateTimeFormatter.ISO_INSTANT.format(source.getUpdate()).replace("Z", "+00:00"));

		//first make a temporary collections of information to allow sorting
		SortedMap<String, SortedSet<String>> attrTypeValue = new TreeMap<>();
		SortedMap<String, SortedMap<String, String>> attrIri = new TreeMap<>();
		SortedMap<String, SortedMap<String, String>> attrUnit = new TreeMap<>();
		
		for (Attribute attribute : source.getCharacteristics()) {
/*
	<Property class="Sample Name" characteristic="false" comment="false"
		type="STRING">
		<QualifiedValue>
			<Value>Test Sample</Value>
			<TermSourceREF>
				<Name />
				<TermSourceID>http://purl.obolibrary.org/obo/NCBITaxon_9606</TermSourceID>
			</TermSourceREF>
			<Unit>year</Unit>
		</QualifiedValue>
	</Property>		
 */
			if (!attrTypeValue.containsKey(attribute.getType())) {
				attrTypeValue.put(attribute.getType(), new TreeSet<>());
				attrIri.put(attribute.getType(), new TreeMap<>());
				attrUnit.put(attribute.getType(), new TreeMap<>());
			}
			attrTypeValue.get(attribute.getType()).add(attribute.getValue());
			
			if (attribute.getIri().size() > 0) {
				String iri = attribute.getIri().first();
				attrIri.get(attribute.getType()).put(attribute.getValue(), iri);
			}

			if (attribute.getUnit() != null && attribute.getUnit().trim().length() > 0) {
				attrUnit.get(attribute.getType()).put(attribute.getValue(), attribute.getUnit());
			}
		}
		for (String attributeType : attrTypeValue.keySet()) {			
			PropertyType property = new PropertyType();
			property.setCharacteristic(false);
			property.setComment(false);
			property.setClazz(attributeType);
			for (String attributeValue : attrTypeValue.get(attributeType)) {	
				QualifiedValueType qualifiedValue = new QualifiedValueType();
				qualifiedValue.setValue(attributeValue);
				
				if (attrIri.get(attributeType).containsKey(attributeValue)) {
					TermSourceREFType termSourceRef = new TermSourceREFType();
					termSourceRef.setTermSourceID(attrIri.get(attributeType).get(attributeValue));
					qualifiedValue.setTermSourceREF(termSourceRef);
				}
				
				if (attrUnit.get(attributeType).containsKey(attributeValue)) {
					qualifiedValue.setUnit(attrUnit.get(attributeType).get(attributeValue));
				}
				
				property.getQualifiedValue().add(qualifiedValue);
			}
			bioSampleType.getProperty().add(property);
		}
		
		//get all the groups this sample is in
		GroupIdsType groupIdsType = new GroupIdsType();
		for (Relationship relationship : source.getRelationships()) {
			if (relationship.getTarget().equals(source.getAccession())
					&& "has member".equals(relationship.getType().toLowerCase())) {
				groupIdsType.getId().add(relationship.getSource());
			}
		}
		bioSampleType.setGroupIds(groupIdsType);

		//get all derived from relationships
		for (Relationship relationship : source.getRelationships()) {
			if (relationship.getSource().equals(source.getAccession())
					&& "derived from".equals(relationship.getType().toLowerCase())) {
				bioSampleType.getDerivedFrom().add(relationship.getTarget());
			}
		}
		
		for (ExternalReference externalReference : source.getExternalReferences()) {			
			DatabaseType databaseType = new DatabaseType();		
			databaseType.setName(externalReferenceService.getNickname(externalReference));
			databaseType.setURI(externalReference.getUrl());
			
			//use the last segment of the URI as the ID
			//not perfect, but good enough?
			List<String> pathSegments = UriComponentsBuilder.fromUriString(externalReference.getUrl()).build().getPathSegments();
			if (pathSegments.size() > 0) {
				databaseType.setID(pathSegments.get(pathSegments.size()-1));
			}
			
			bioSampleType.getDatabase().add(databaseType);
		}
		
		
		return bioSampleType;
	}

}
