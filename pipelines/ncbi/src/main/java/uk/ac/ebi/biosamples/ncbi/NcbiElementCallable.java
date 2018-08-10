package uk.ac.ebi.biosamples.ncbi;

import java.util.concurrent.Callable;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.hateoas.Resource;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.ncbi.service.NcbiSampleConversionService;

public class NcbiElementCallable implements Callable<Void> {
	
	private Logger log = LoggerFactory.getLogger(getClass());

	private final Element sampleElem;

	private final String domain;
	
	private final BioSamplesClient bioSamplesClient;

	private final NcbiSampleConversionService ncbiSampleConversionService;

	public NcbiElementCallable(NcbiSampleConversionService ncbiSampleConversionService, BioSamplesClient bioSamplesClient, Element sampleElem, String domain) {
		this.ncbiSampleConversionService = ncbiSampleConversionService;
		this.bioSamplesClient = bioSamplesClient;
		this.sampleElem = sampleElem;
		this.domain = domain;
	}

	@Override
	public Void call() throws Exception {

		String accession = sampleElem.attributeValue("accession");

		log.trace("Element callable starting for "+accession);

		// Generate the sample without the domain
		Sample sampleWithoutDomain = this.ncbiSampleConversionService.convertNcbiXmlElementToSample(sampleElem);

		// Attach the domain
		Sample sample = Sample.Builder.fromSample(sampleWithoutDomain).withDomain(domain).build();

		//now pass it along to the actual submission process
		bioSamplesClient.persistSampleResource(sample);

		log.trace("Element callable finished");
		
		return null;
	}
	
	/**
	 * Safe way to extract the taxonomy id from the string
	 * @param value
	 * @return
	 */
	private int getTaxId(String value) {
		if (value == null) {
			throw new RuntimeException("Unable to extract tax id from a null value");
		}
		return Integer.parseInt(value.trim());
	}

}
