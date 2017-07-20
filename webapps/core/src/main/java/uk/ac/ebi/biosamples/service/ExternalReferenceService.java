package uk.ac.ebi.biosamples.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.mongo.model.MongoExternalReference;
import uk.ac.ebi.biosamples.mongo.repo.MongoExternalReferenceRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoExternalReferenceToExternalReferenceConverter;

@Service
public class ExternalReferenceService {

	@Autowired
	private MongoExternalReferenceRepository mongoExternalReferenceRepository;
	
	@Autowired
	private MongoExternalReferenceToExternalReferenceConverter mongoExternalReferenceToExternalReferenceConverter;

	
	public Page<ExternalReference> getPage(Pageable pageable){
		Page<MongoExternalReference> pageNeoExternalReference = mongoExternalReferenceRepository.findAll(pageable);
		Page<ExternalReference> pageExternalReference = pageNeoExternalReference.map(mongoExternalReferenceToExternalReferenceConverter);		
		return pageExternalReference;
	}
	
	public ExternalReference getExternalReference(String urlHash) {
		MongoExternalReference neoExternalReference = mongoExternalReferenceRepository.findOne(urlHash);
		if (neoExternalReference == null) {
			return null;
		} else {
			return mongoExternalReferenceToExternalReferenceConverter.convert(neoExternalReference);
		}
	}
	
	
}
