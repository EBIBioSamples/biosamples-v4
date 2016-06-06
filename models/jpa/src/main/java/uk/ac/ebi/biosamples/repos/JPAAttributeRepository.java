package uk.ac.ebi.biosamples.repos;


import org.springframework.data.domain.Page;
import org.springframework.data.repository.PagingAndSortingRepository;

import uk.ac.ebi.biosamples.models.JPAAttribute;

public interface JPAAttributeRepository extends PagingAndSortingRepository<JPAAttribute, Long> {

	Page<JPAAttribute> findByKeyAndValueAndUnitAndOntologyTerm(String key, String value, String unit, String ontologyTerm);
}
