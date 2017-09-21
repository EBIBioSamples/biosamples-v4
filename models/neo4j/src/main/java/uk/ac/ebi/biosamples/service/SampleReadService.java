package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;
import uk.ac.ebi.biosamples.neo.service.modelconverter.NeoSampleToSampleConverter;

/**
 * Service layer business logic for centralising repository access and
 * conversions between different controller. Use this instead of linking to
 * repositories directly.
 * 
 * @author faulcon
 *
 */
@Service
public class SampleReadService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private NeoSampleRepository neoSampleRepository;
	
	//TODO use a ConversionService to manage all these
	@Autowired
	private NeoSampleToSampleConverter neoSampleToSampleConverter;
	
	@Autowired
	private CurationReadService curationReadService;

	/**
	 * Throws an IllegalArgumentException of no sample with that accession exists
	 * 
	 * @param accession
	 * @return
	 * @throws IllegalArgumentException
	 */
	//can't use a sync cache because we need to use CacheEvict
	//@Cacheable(cacheNames=WebappProperties.fetch, key="#root.args[0]")
	public Sample fetch(String accession) throws SampleNotFoundException {
		
		log.info("Fetching accession from neoSampleRepository "+accession);
		
		// return the raw sample from the repository
		NeoSample neoSample = neoSampleRepository.findOneByAccession(accession,1);
		if (neoSample == null) {
			throw new SampleNotFoundException(accession);
		}
		//TODO only have relationships to things that are accessible

		// convert it into the format to return
		Sample sample = neoSampleToSampleConverter.convert(neoSample);
		
		//add curation from a set of users
		//TODO limit curation to a set of users (possibly empty set)
		sample = curationReadService.getAndApplyCurationsToSample(sample);

		return sample;
	}


	@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such Sample") // 404
	public static class SampleNotFoundException extends RuntimeException {

		public SampleNotFoundException(String accession) {
			super("Unable to find accession "+accession);
		}
	}
	
}
