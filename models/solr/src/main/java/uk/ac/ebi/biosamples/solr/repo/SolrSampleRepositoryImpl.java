package uk.ac.ebi.biosamples.solr.repo;

import java.io.Serializable;
import java.util.Collection;

import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.SolrTransactionSynchronizationAdapterBuilder;
import org.springframework.data.solr.repository.support.SimpleSolrRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import uk.ac.ebi.biosamples.solr.model.SolrSample;

@Repository
public class SolrSampleRepositoryImpl implements SolrSampleRepositoryCustom {

	private SolrOperations solrOperations;
	
	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * @param solrOperations must not be null
	 */
	public SolrSampleRepositoryImpl(SolrOperations samplesSolrOperations) {
		//Assert.notNull(solrClient);

		this.setSolrOperations(samplesSolrOperations);
	}

	public final void setSolrOperations(SolrOperations solrOperations) {
		Assert.notNull(solrOperations, "SolrOperations must not be null.");

		this.solrOperations = solrOperations;
	}

	public final SolrOperations getSolrOperations() {
		return solrOperations;
	}

	
	@Override
	public SolrSample saveWithin(SolrSample entity, int commitWithinMs) {
		Assert.notNull(entity, "Cannot save 'null' entity.");
		this.solrOperations.saveBean(entity, commitWithinMs);
		return entity;
	}

	@Override
	public Iterable<SolrSample> saveWithin(Iterable<SolrSample> entities, int commitWithinMs) {
		Assert.notNull(entities, "Cannot insert 'null' as a List.");
		
		if (!(entities instanceof Collection<?>)) {
			throw new InvalidDataAccessApiUsageException("Entities have to be inside a collection");
		}

		this.solrOperations.saveBeans((Collection<? extends SolrSample>) entities, commitWithinMs);
		return entities;
	}
	
}
