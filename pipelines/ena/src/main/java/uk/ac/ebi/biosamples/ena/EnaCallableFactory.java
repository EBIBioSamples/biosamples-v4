package uk.ac.ebi.biosamples.ena;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.structured.AbstractData;

import java.util.Set;
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

    /**
     * Builds callable for dealing most ENA samples
     *
     * @param accession           The accession passed
     * @param  suppressionHandler Is running to set samples to SUPPRESSED
     * @param  bsdAuthority       Indicates its running for samples submitted through BioSamples
     * @param  amrData            The AMR {@link AbstractData} of the sample
     * @return the callable, {@link EnaCallable}
     */
    public Callable<Void> build(String accession, boolean suppressionHandler, boolean bsdAuthority, Set<AbstractData> amrData) {
        return new EnaCallable(accession, bioSamplesClient, enaXmlEnhancer,
                enaElementConverter, eraProDao, domain, suppressionHandler, bsdAuthority, amrData);
    }
}
