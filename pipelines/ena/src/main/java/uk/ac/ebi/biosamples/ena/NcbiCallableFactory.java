package uk.ac.ebi.biosamples.ena;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;

@Service
public class NcbiCallableFactory {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final BioSamplesClient bioSamplesClient;

	public NcbiCallableFactory(BioSamplesClient bioSamplesClient) {

		this.bioSamplesClient = bioSamplesClient;
	}
	
	public NcbiCallable build(String accession) {
		return new NcbiCallable(accession, bioSamplesClient);
	}
}
