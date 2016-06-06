package uk.ac.ebi.biosamples.repos;

import org.springframework.data.repository.PagingAndSortingRepository;

import uk.ac.ebi.biosamples.models.JPASample;

public interface JPASampleRepository extends PagingAndSortingRepository<JPASample, Long> {

	public JPASample findByAccession(String accession);
}
