package uk.ac.ebi.biosamples.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReferenceApplication;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoExternalReferenceRepository;
import uk.ac.ebi.biosamples.neo.service.modelconverter.ExternalReferenceToNeoExternalReferenceConverter;
import uk.ac.ebi.biosamples.neo.service.modelconverter.NeoExternalReferenceToExternalReferenceConverter;

@Service
public class ExternalReferenceService {

	@Autowired
	private NeoExternalReferenceRepository neoExternalReferenceRepository;
	
	@Autowired
	private NeoExternalReferenceToExternalReferenceConverter neoExternalReferenceToExternalReferenceConverter;
	
	@Autowired
	private ExternalReferenceToNeoExternalReferenceConverter externalReferenceToNeoExternalReferenceConverter;
	
	public Page<ExternalReference> getPage(Pageable pageable){
		Page<NeoExternalReference> pageNeoExternalReference = neoExternalReferenceRepository.findAll(pageable);
		Page<ExternalReference> pageExternalReference = pageNeoExternalReference.map(neoExternalReferenceToExternalReferenceConverter);		
		return pageExternalReference;
	}
	
	public ExternalReference getExternalReference(String urlHash) {
		NeoExternalReference neoExternalReference = neoExternalReferenceRepository.findOneByUrlHash(urlHash);
		if (neoExternalReference == null) {
			return null;
		} else {
			return neoExternalReferenceToExternalReferenceConverter.convert(neoExternalReference);
		}
	}
	
	public Page<ExternalReference> getExternalReferencesOfSample(String accession, Pageable pageable) {
		Page<NeoExternalReference> pageNeoExternalReference = neoExternalReferenceRepository.findBySampleAccession(accession, pageable);
		Page<ExternalReference> pageExternalReference = pageNeoExternalReference.map(neoExternalReferenceToExternalReferenceConverter);
		return pageExternalReference;
	}

	public ExternalReference store(ExternalReference externalReference, String sampleAccession) {

		// convert it to the storage specific version
		NeoExternalReference neoExternalReference = externalReferenceToNeoExternalReferenceConverter.convert(externalReference);
		neoExternalReference.getApplications().add(NeoExternalReferenceApplication.build(NeoSample.create(sampleAccession), neoExternalReference));
		
		synchronized(this) {
			neoExternalReference = neoExternalReferenceRepository.save(neoExternalReference);
		}
		
		return neoExternalReferenceToExternalReferenceConverter.convert(neoExternalReference);
	}
}
