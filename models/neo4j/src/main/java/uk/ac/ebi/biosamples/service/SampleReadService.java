package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
	public Sample fetch(String accession) throws IllegalArgumentException {
		
		log.info("Fetching accession from neoSampleRepository "+accession);
		
		// return the raw sample from the repository
		NeoSample neoSample = neoSampleRepository.findOneByAccession(accession,1);
		if (neoSample == null) {
			throw new IllegalArgumentException("Unable to find sample (" + accession + ")");
		}
		//TODO only have relationships to things that are accessible

		// convert it into the format to return
		Sample sample = neoSampleToSampleConverter.convert(neoSample);
		
		//add curation from a set of users
		//TODO limit curation to a set of users (possibly empty set)
		sample = curationReadService.applyAllCurationToSample(sample);
		
		
		return sample;
	}
	
	
}
