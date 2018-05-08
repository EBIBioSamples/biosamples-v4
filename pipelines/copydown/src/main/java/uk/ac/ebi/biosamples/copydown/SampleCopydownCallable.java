package uk.ac.ebi.biosamples.copydown;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

public class SampleCopydownCallable implements Callable<Void> {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final Sample sample;
	private final BioSamplesClient bioSamplesClient;
	private final String domain;

	public static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<String>();
	private static final Attribute mixedAttribute = Attribute.build("organism", "mixed sample", "http://purl.obolibrary.org/obo/NCBITaxon_1427524", null);
	
	public SampleCopydownCallable(BioSamplesClient bioSamplesClient, Sample sample, String domain) {
		this.bioSamplesClient = bioSamplesClient;
		this.sample = sample;
		this.domain = domain;
	}
	
	@Override
	public Void call() throws Exception {
		
		boolean hasOrganism = false;
		for (Attribute attribute : sample.getAttributes()) {
			if ("organism".equals(attribute.getType().toLowerCase())) {
				hasOrganism = true;
			}
		}
		
		boolean hasDerivedFrom = false;
		for (Relationship relationship : sample.getRelationships()) {
			if ("derived from".equals(relationship.getType().toLowerCase())
					&& sample.getAccession().equals(relationship.getSource())) {
				hasDerivedFrom = true;
			}
		}
		
		if (!hasOrganism && hasDerivedFrom) {
			//walk up the derived from relationships and pull out all the organisms
			Set<Attribute> organisms = getOrganismsForSample(sample, false);
			if (organisms.size() > 1) {
				//if there are multiple organisms, use a "mixed sample" taxonomy reference
				//some users expect one taxonomy reference, no more, no less
				log.debug("Applying curation to "+sample.getAccession());
				applyCuration(mixedAttribute);
			} else if (organisms.size() == 1) {
				log.debug("Applying curation to "+sample.getAccession());
				applyCuration(organisms.iterator().next());
			} else {
				log.warn("Unable to find organism for "+sample.getAccession());
			}
		} else if (hasOrganism && hasDerivedFrom) {
			//this sample has an organism, but that might have been applied by a previous curation
			for (Resource<CurationLink> curationLink : bioSamplesClient.fetchCurationLinksOfSample(sample.getAccession())) {
				if (domain.equals(curationLink.getContent().getDomain())) {
					SortedSet<Attribute> attributesPre = curationLink.getContent().getCuration().getAttributesPre();
					SortedSet<Attribute> attributesPost = curationLink.getContent().getCuration().getAttributesPost();
					//check that this is as structured as expected
					if (attributesPre.size() != 0) {
						throw new RuntimeException("Expected no pre attribute, got "+attributesPre.size());
					}
					if (attributesPost.size() != 1) {
						throw new RuntimeException("Expected single post attribute, got "+attributesPost.size());
					}
					//this curation link was applied by us, check it is still valid
					Set<Attribute> organisms = getOrganismsForSample(sample, true);
					if (organisms.size() > 1) {
						//check if the postattribute is the same as the organisms
						String organism = "mixed sample";
						if (!organism.equals(attributesPost.iterator().next().getValue())) {
							log.debug("Replacing curation on "+sample.getAccession()+" with \"mixed Sample\"");
							bioSamplesClient.deleteCurationLink(curationLink.getContent());
							applyCuration(mixedAttribute);
						}
					} else if (organisms.size() == 1) {
						//check if the postattribute is the same as the organisms
						Attribute organism = organisms.iterator().next();
						if (!organism.equals(attributesPost.iterator().next().getValue())) {
							log.debug("Replacing curation on "+sample.getAccession()+" with "+organism);
							bioSamplesClient.deleteCurationLink(curationLink.getContent());
							applyCuration(organism);
						}
					}
				}
			}
		}
		
		return null;
	}
	
	private void applyCuration(Attribute organismValue) {
		Set<Attribute> postAttributes = new HashSet<>();
		postAttributes.add(organismValue);
		Curation curation = Curation.build(Collections.emptyList(), 
				postAttributes);
		bioSamplesClient.persistCuration(sample.getAccession(), curation, domain);
	}
	
	private Set<Attribute> getOrganismsForSample(Sample sample, boolean ignoreSample) {
		Set<Attribute> organisms = new HashSet<>();
		if (!ignoreSample) {
			for (Attribute attribute : sample.getAttributes()) {
				if ("organism".equals(attribute.getType().toLowerCase())) {
					organisms.add(attribute);
				}
			}
		}
		//if there are no organisms directly, check derived from relationships
		if (organisms.size() == 0) {
			log.trace(""+sample.getAccession()+" has no organism");
			for (Relationship relationship : sample.getRelationships()) {
				if ("derived from".equals(relationship.getType().toLowerCase())
						&& sample.getAccession().equals(relationship.getSource())) {
					log.trace("checking derived from "+relationship.getTarget());
					Optional<Resource<Sample>> derivedFrom = bioSamplesClient.fetchSampleResource(relationship.getTarget());
					if (derivedFrom.isPresent()) {
						//recursion ahoy!
						organisms.addAll(getOrganismsForSample(derivedFrom.get().getContent(), false));
					}
				}
			}
		}
		return organisms;
	}

}
