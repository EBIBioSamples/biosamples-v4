package uk.ac.ebi.biosamples.ena;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;

import java.util.concurrent.Callable;

@Service
public class EnaCallableFactory {
    private final BioSamplesClient bioSamplesClient;
    private final EnaXmlEnhancer enaXmlEnhancer;
    private final EnaElementConverter enaElementConverter;
    private final EraProDao eraProDao;
    private final String domain;

    public EnaCallableFactory(BioSamplesClient bioSamplesClient, EnaXmlEnhancer enaXmlEnhancer,
                              EnaElementConverter enaElementConverter, EraProDao eraProDao, PipelinesProperties pipelinesProperties) {

        this.bioSamplesClient = bioSamplesClient;
        this.enaXmlEnhancer = enaXmlEnhancer;
        this.enaElementConverter = enaElementConverter;
        this.eraProDao = eraProDao;
        this.domain = pipelinesProperties.getEnaDomain();
    }

    public Callable<Void> build(String accession) {
		 return new EnaCallable(accession, bioSamplesClient, enaXmlEnhancer,
	                enaElementConverter, eraProDao, domain);
	}

    /**
	 * Builds a callable for dealing samples that are SUPPRESSED
	 * 
	 * @param accession
	 * 			The accession passed
	 * @param suppressionHandler
	 * 			true for this case
	 * @return the callable, {@link EnaCallable}
	 */
    public EnaCallable build(String accession, boolean suppressionHandler) {
        return new EnaCallable(accession, bioSamplesClient, enaXmlEnhancer,
                enaElementConverter, eraProDao, domain, suppressionHandler);
    }
}
