package uk.ac.ebi.biosamples.ena;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;

@Service
public class EnaCallableFactory {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final BioSamplesClient bioSamplesClient;
	private final EnaElementConverter enaElementConverter;
	private final EraProDao eraProDao;
	private final String domain;

	public EnaCallableFactory(BioSamplesClient bioSamplesClient,
			EnaElementConverter enaElementConverter, EraProDao eraProDao, PipelinesProperties pipelinesProperties) {

		this.bioSamplesClient = bioSamplesClient;
		this.enaElementConverter = enaElementConverter;
		this.eraProDao = eraProDao;
		this.domain = pipelinesProperties.getEnaDomain();
	}
	
	public EnaCallable build(String accession) {
		return new EnaCallable(accession, bioSamplesClient,
				enaElementConverter, eraProDao, domain);
	}
}
