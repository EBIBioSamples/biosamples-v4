package uk.ac.ebi.biosamples.ena;

import java.util.Collections;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.ExternalReference;

public class NcbiCurationCallable implements Callable<Void> {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final String accession;
	private final BioSamplesClient bioSamplesClient;
	
	public NcbiCurationCallable(String accession, BioSamplesClient bioSamplesClient) {
		this.accession = accession;
		this.bioSamplesClient = bioSamplesClient;
	}
	
	@Override
	public Void call() throws Exception {
		log.trace("HANDLING " + accession);
		ExternalReference exRef = ExternalReference.build("https://www.ebi.ac.uk/ena/data/view/"+accession);
		Curation curation = Curation.build(null, null, null, Collections.singleton(exRef));
		bioSamplesClient.persistCuration(accession, curation, null);
		log.trace("HANDLED " + accession);
		return null;
	}

}
