package uk.ac.ebi.biosamples.ena;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.client.BioSamplesClient;

@Service
public class NcbiCurationCallableFactory {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final BioSamplesClient bioSamplesClient;

	public NcbiCurationCallableFactory(BioSamplesClient bioSamplesClient) {

		this.bioSamplesClient = bioSamplesClient;
	}
	
	public NcbiCurationCallable build(String accession) {
		return new NcbiCurationCallable(accession, bioSamplesClient);
	}
}
