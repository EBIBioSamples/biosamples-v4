package uk.ac.ebi.biosamples.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;
import uk.ac.ebi.biosamples.neo.repo.NeoExternalReferenceRepository;
import uk.ac.ebi.biosamples.neo.service.modelconverter.NeoExternalReferenceToExternalReferenceConverter;

@Service
public class ExternalReferenceService {

	@Autowired
	private NeoExternalReferenceRepository neoExternalReferenceRepository;
	
	@Autowired
	private NeoExternalReferenceToExternalReferenceConverter neoExternalReferenceToExternalReferenceConverter;
	
	public Page<ExternalReference> getPage(Pageable pageable){
		Page<NeoExternalReference> pageNeo = neoExternalReferenceRepository.findAll(pageable);
		
		List<ExternalReference> externalReferences = new ArrayList<>();
		for (NeoExternalReference neoExternalReference : pageNeo) {
			externalReferences.add(neoExternalReferenceToExternalReferenceConverter.convert(neoExternalReference));
		}

		Page<ExternalReference> pageExternalReference = new PageImpl<>(externalReferences,pageable, pageNeo.getTotalElements());
		
		return pageExternalReference;
	}
	
	public ExternalReference getExternalReference(String id) {
		NeoExternalReference neoExternalReference = neoExternalReferenceRepository.findOneByUrlHash(id);
		if (neoExternalReference == null) {
			return null;
		} else {
			return neoExternalReferenceToExternalReferenceConverter.convert(neoExternalReference);
		}
	}
}
