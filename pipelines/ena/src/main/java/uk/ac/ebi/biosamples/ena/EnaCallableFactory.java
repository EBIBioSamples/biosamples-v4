package uk.ac.ebi.biosamples.ena;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.biosamples.client.BioSamplesClient;

@Service
public class EnaCallableFactory {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final BioSamplesClient bioSamplesClient;
	private final RestTemplate restTemplate;
	private final EnaElementConverter enaElementConverter;
	private final EraProDao eraProDao;

	public EnaCallableFactory(BioSamplesClient bioSamplesClient, RestTemplateBuilder restTemplatebuilder,
			EnaElementConverter enaElementConverter, EraProDao eraProDao) {

		this.bioSamplesClient = bioSamplesClient;
		this.restTemplate = restTemplatebuilder.build();
		this.enaElementConverter = enaElementConverter;
		this.eraProDao = eraProDao;
	}
	
	public EnaCallable build(String accession) {
		return new EnaCallable(accession, bioSamplesClient, restTemplate,
				enaElementConverter, eraProDao);
	}
}
