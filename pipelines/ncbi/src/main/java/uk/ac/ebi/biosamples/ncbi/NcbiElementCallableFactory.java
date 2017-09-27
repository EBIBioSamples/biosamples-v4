package uk.ac.ebi.biosamples.ncbi;

import org.dom4j.Element;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.utils.TaxonomyService;

@Service
public class NcbiElementCallableFactory {

	
	private final TaxonomyService taxonomyService;
	
	private final BioSamplesClient bioSamplesClient;

	private final String domain;
	

	public NcbiElementCallableFactory(TaxonomyService taxonomyService, BioSamplesClient bioSamplesClient, PipelinesProperties pipelinesProperties) {
		this.taxonomyService = taxonomyService;
		this.bioSamplesClient = bioSamplesClient;
		this.domain = pipelinesProperties.getNcbiDomain();
	}
	
	
	public NcbiElementCallable build(Element element) {
		return new NcbiElementCallable(taxonomyService, bioSamplesClient, element, domain);
	}
}
