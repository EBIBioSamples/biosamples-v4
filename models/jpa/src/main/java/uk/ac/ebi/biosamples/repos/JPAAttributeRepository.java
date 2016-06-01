package uk.ac.ebi.biosamples.repos;

import org.springframework.data.repository.CrudRepository;

import uk.ac.ebi.biosamples.models.JPAAttribute;

public interface JPAAttributeRepository extends CrudRepository<JPAAttribute, Long> {

	Iterable<JPAAttribute> findByKeyAndValueAndUnitAndOntologyTerm(String key, String value, String unit, String ontologyTerm);
}
