package uk.ac.ebi.biosamples.copydown;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.CurationApplicationService;

public class SampleCopydownCallable implements Callable<Void> {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final Sample sample;
	private final BioSamplesClient bioSamplesClient;
	private final String domain;

	public static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<String>();
	
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
			Set<String> organisms = getOrganismsForSample(sample);
			if (organisms.size() > 0) {
				Set<Attribute> postAttributes = new HashSet<>();
				for (String organism : organisms) {
					postAttributes.add(Attribute.build("Organism", organism));
				}
				Curation curation = Curation.build(Collections.emptyList(), 
						postAttributes);
				bioSamplesClient.persistCuration(sample.getAccession(), curation, domain);
			}
		}
		
		return null;
	}
	
	public Set<String> getOrganismsForSample(Sample sample) {
		Set<String> organisms = new HashSet<>();
		for (Attribute attribute : sample.getAttributes()) {
			if ("organism".equals(attribute.getType().toLowerCase())) {
				organisms.add(attribute.getValue());
			}
		}
		//if there are no organisms directly, check derived from relationships
		if (organisms.size() == 0) {
			for (Relationship relationship : sample.getRelationships()) {
				if ("derived from".equals(relationship.getType().toLowerCase())
						&& sample.getAccession().equals(relationship.getSource())) {
					Optional<Resource<Sample>> derivedFrom = bioSamplesClient.fetchSampleResource(relationship.getTarget());
					if (derivedFrom.isPresent()) {
						//recursion ahoy!
						organisms.addAll(getOrganismsForSample(derivedFrom.get().getContent()));
					}
				}
			}
		}
		return organisms;
	}

}
