package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

@Service
public class CurationApplicationService {

	private Logger log = LoggerFactory.getLogger(getClass());

	
	public Sample applyCurationToSample(Sample sample, Curation curation) {
		log.trace("Applying curation "+curation+" to sample "+sample);
		
		SortedSet<Attribute> attributes = new TreeSet<Attribute>(sample.getAttributes());
		SortedSet<ExternalReference> externalReferences = new TreeSet<ExternalReference>(sample.getExternalReferences());
		//remove pre-curation things
		for (Attribute attribute : curation.getAttributesPre()) {
			if (!attributes.contains(attribute)) {
				throw new IllegalArgumentException("Attempting to apply curation "+curation+" to sample "+sample);
			}
			attributes.remove(attribute);
		}
		for (ExternalReference externalReference : curation.getExternalReferencesPre()) {
			if (!externalReferences.contains(externalReference)) {
				throw new IllegalArgumentException("Attempting to apply curation "+curation+" to sample "+sample);
			}
			externalReferences.remove(externalReference);
		}
		//add post-curation things
		for (Attribute attribute : curation.getAttributesPost()) {
			if (attributes.contains(attribute)) {
				throw new IllegalArgumentException("Attempting to apply curation "+curation+" to sample "+sample);
			}
			attributes.add(attribute);
		}
		for (ExternalReference externalReference : curation.getExternalReferencesPost()) {
			if (externalReferences.contains(externalReference)) {
				throw new IllegalArgumentException("Attempting to apply curation "+curation+" to sample "+sample);
			}
			externalReferences.add(externalReference);
		}
		
		return Sample.build(sample.getName(), sample.getAccession(), sample.getDomain(), 
				sample.getRelease(), sample.getUpdate(), attributes, sample.getData(), sample.getRelationships(), externalReferences,
				sample.getOrganizations(), sample.getContacts(), sample.getPublications(), sample.getSubmittedVia());
	}
	
	public Sample applyAllCurationToSample(Sample sample, Collection<Curation> curations) {

		boolean curationApplied = true;
		while (curationApplied && curations.size() > 0) {
			Iterator<Curation> it = curations.iterator();
			curationApplied = false;
			while (it.hasNext()) {
				Curation curation = it.next();
				try {
					sample = applyCurationToSample(sample, curation);
					it.remove();
					curationApplied = true;
				} catch (IllegalArgumentException e) {
					//do nothing, will try again next loop
				}
			}
		}
		if (!curationApplied) {
			//we stopped because we didn't apply any curation
			//therefore we have some curations that can't be applied
			//this is a warning
			log.debug("Unapplied curation on {}", sample.getAccession());
		}
		return sample;
	}
}
