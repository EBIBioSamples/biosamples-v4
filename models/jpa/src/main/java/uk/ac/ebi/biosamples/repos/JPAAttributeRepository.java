package uk.ac.ebi.biosamples.repos;

import org.springframework.data.repository.CrudRepository;

import uk.ac.ebi.biosamples.models.JPAAttribute;

public interface JPAAttributeRepository extends CrudRepository<JPAAttribute, Long> {

	Iterable<JPAAttribute> findByTypeAndValueAndUnitAndOntologyTerm(String type, String value, String unit, String ontologyTerm);
}
