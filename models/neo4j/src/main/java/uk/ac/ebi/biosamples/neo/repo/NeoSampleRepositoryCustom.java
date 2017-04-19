package uk.ac.ebi.biosamples.neo.repo;

import uk.ac.ebi.biosamples.neo.model.NeoSample;

public interface NeoSampleRepositoryCustom {
	
	public NeoSample insertNew(NeoSample sample);

}
