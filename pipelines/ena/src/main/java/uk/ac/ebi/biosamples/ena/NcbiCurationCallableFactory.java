package uk.ac.ebi.biosamples.ena;

import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;

@Service
public class NcbiCurationCallableFactory {
	private final BioSamplesClient bioSamplesClient;
	private final String domain;

	public NcbiCurationCallableFactory(BioSamplesClient bioSamplesClient, PipelinesProperties pipelinesProperties) {

		this.bioSamplesClient = bioSamplesClient;
		this.domain = pipelinesProperties.getEnaDomain();
	}
	
	public NcbiCurationCallable build(String accession) {
		return new NcbiCurationCallable(accession, bioSamplesClient, domain);
	}

	/**
	 * Builds a callable for dealing samples that are SUPPRESSED
	 * 
	 * @param accession
	 * 			The accession passed
	 * @param suppressionHandler
	 * 			true for this case
	 * @return the callable, {@link NcbiCurationCallable}
	 */
	public NcbiCurationCallable build(String accession, boolean suppressionHandler) {
		return new NcbiCurationCallable(accession, bioSamplesClient, domain, suppressionHandler);
	}
}
