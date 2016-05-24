package uk.ac.ebi.biosamples.repos;

import org.springframework.data.repository.CrudRepository;

import uk.ac.ebi.biosamples.models.JPASample;

public interface JPASampleRepository extends CrudRepository<JPASample, Long> {

	public JPASample findByAccession(String accession);
}
