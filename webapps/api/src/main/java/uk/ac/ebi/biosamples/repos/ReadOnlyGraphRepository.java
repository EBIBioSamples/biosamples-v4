package uk.ac.ebi.biosamples.repos;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;


@NoRepositoryBean
public interface ReadOnlyGraphRepository<T> extends ReadOnlyRepository<T, Long> {

    T findOne(Long id, int depth);

    Iterable<T> findAll(int depth);

    Iterable<T> findAll(Sort sort, int depth);

    Iterable<T> findAll(Iterable<Long> ids, int depth);

    Iterable<T> findAll(Iterable<Long> ids, Sort sort);

    Iterable<T> findAll(Iterable<Long> ids, Sort sort, int depth);


    Page<T> findAll(Pageable pageable, int depth);

}
