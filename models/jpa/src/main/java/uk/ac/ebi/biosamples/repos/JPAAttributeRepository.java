package uk.ac.ebi.biosamples.repos;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import uk.ac.ebi.biosamples.models.JPAAttribute;

public interface JPAAttributeRepository extends PagingAndSortingRepository<JPAAttribute, Long> {

	public Iterable<JPAAttribute> findByKeyAndValueAndUnitAndOntologyTerm(String key, String value, String unit, String ontologyTerm);
	
	public Page<JPAAttribute> findByKeyAndValueAndUnitAndOntologyTerm(String key, String value, String unit, String ontologyTerm, Pageable pageable);
}
