package uk.ac.ebi.biosamples.ena;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;

public class NcbiCurationCallable implements Callable<Void> {
	private static final String SUPPRESSED = "suppressed";

	private Logger log = LoggerFactory.getLogger(getClass());

	private final String accession;
	private final BioSamplesClient bioSamplesClient;
	private final String domain;
	private boolean suppressionHandler;

	public NcbiCurationCallable(String accession, BioSamplesClient bioSamplesClient, String domain) {
		this.accession = accession;
		this.bioSamplesClient = bioSamplesClient;
		this.domain = domain;
	}

	/**
	 * Construction for SUPPRESSED samples
	 * 
	 * @param accession
	 * @param bioSamplesClient
	 * @param domain
	 * @param suppressionHandler
	 */
	public NcbiCurationCallable(String accession, BioSamplesClient bioSamplesClient, String domain,
			boolean suppressionHandler) {
		this.accession = accession;
		this.bioSamplesClient = bioSamplesClient;
		this.domain = domain;
		this.suppressionHandler = suppressionHandler;
	}

	@Override
	public Void call() throws Exception {
		log.trace("HANDLING " + accession);
		ExternalReference exRef = ExternalReference.build("https://www.ebi.ac.uk/ena/data/view/" + accession);
		Curation curation = Curation.build(null, null, null, Collections.singleton(exRef));

		if (suppressionHandler) {
			checkAndUpdateSuppressedSample(accession);
		} else {
			// get the sample to make sure it exists first
			if (bioSamplesClient.fetchSampleResource(accession, Optional.empty()).isPresent()) {
				bioSamplesClient.persistCuration(accession, curation, domain);
			} else {
				log.warn("Unable to find " + accession);
			}
		}
		log.trace("HANDLED " + accession);
		return null;
	}

	/**
	 * Checks if sample status is not SUPPRESSED in BioSamples, if yes then persists the sample with SUPPRESSED status
	 * 
	 * @param sampleAccession
	 * 				The accession passed
	 */
	private void checkAndUpdateSuppressedSample(String sampleAccession) {
		final Optional<Resource<Sample>> optionalSampleResource = bioSamplesClient.fetchSampleResource(sampleAccession, Optional.empty());

		if (optionalSampleResource.isPresent()) {
			final Sample sample = optionalSampleResource.get().getContent();
			boolean persistRequired = true;

			for (Attribute attribute : sample.getAttributes()) {
				if (attribute.getType().equals("INSDC status") && attribute.getValue().equals(SUPPRESSED)) {
					persistRequired = false;
					break;
				}
			}

			if (persistRequired) {
				sample.getAttributes().removeIf(attr -> attr.getType().contains("INSDC status"));
				sample.getAttributes().add(Attribute.build("INSDC status", SUPPRESSED));
				log.info("Updating status to suppressed of sample: " + sampleAccession);
				bioSamplesClient.persistSampleResource(sample);
			}
		}
	}
}
