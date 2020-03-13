package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.ac.ebi.biosamples.model.Autocomplete;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.StaticViewWrapper;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoAccessionService;
import uk.ac.ebi.biosamples.mongo.service.MongoInverseRelationshipService;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;
import uk.ac.ebi.biosamples.solr.service.SolrSampleService;

import java.time.Instant;
import java.util.*;

/**
 * Service layer business logic for centralising repository access and
 * conversions between different controller. Use this instead of linking to
 * repositories directly.
 *
 * @author faulcon
 */
@Service
public class SampleService {

    private static Logger log = LoggerFactory.getLogger(SampleService.class);

    //TODO use constructor injection

    @Autowired
    private MongoAccessionService mongoAccessionService;
    @Autowired
    private MongoSampleRepository mongoSampleRepository;
    @Autowired
    private MongoSampleToSampleConverter mongoSampleToSampleConverter;
    @Autowired
    private SampleToMongoSampleConverter sampleToMongoSampleConverter;
    @Autowired
    private MongoInverseRelationshipService mongoInverseRelationshipService;


    @Autowired
    private SampleValidator sampleValidator;

    @Autowired
    private SolrSampleService solrSampleService;

    @Autowired
    private SampleReadService sampleReadService;

    @Autowired
    private MessagingService messagingSerivce;

    /**
     * Throws an IllegalArgumentException of no sample with that accession exists
     *
     * @param accession
     * @return
     * @throws IllegalArgumentException
     */
    //can't use a sync cache because we need to use CacheEvict
    //@Cacheable(cacheNames=WebappProperties.fetchUsing, key="#root.args[0]")
    public Optional<Sample> fetch(String accession, Optional<List<String>> curationDomains, String curationRepo) {
        StaticViewWrapper.StaticView staticView = StaticViewWrapper.getStaticView(curationDomains.orElse(null), curationRepo);
        return sampleReadService.fetch(accession, curationDomains, staticView);
    }

    public Autocomplete getAutocomplete(String autocompletePrefix, Collection<Filter> filters, int noSuggestions) {
        return solrSampleService.getAutocomplete(autocompletePrefix, filters, noSuggestions);
    }

    //because the fetchUsing caches the sample, if an updated version is stored, we need to make sure that any cached version
    //is removed.
    //Note, pages of samples will not be cache busted, only single-accession sample retrieval
    //@CacheEvict(cacheNames=WebappProperties.fetchUsing, key="#result.accession")
    public Sample store(Sample sample) {
        //do validation
        // TODO validate that relationships have this sample as the source
        Collection<String> errors = sampleValidator.validate(sample);
        if (errors.size() > 0) {
            //TODO no validation information is provided to users
            log.error("Found errors : " + errors);
            throw new SampleValidationException();
        }

        if (sample.hasAccession()) {
            MongoSample mongoOldSample = mongoSampleRepository.findOne(sample.getAccession());
            List<String> existingRelationshipTargets = new ArrayList<>();
            if (mongoOldSample != null) {
                Sample oldSample = mongoSampleToSampleConverter.convert(mongoOldSample);
                existingRelationshipTargets = getExistingRelationshipTargets(sample.getAccession(), mongoOldSample);
                sample = compareWithExistingAndUpdateSample(sample, oldSample);
            } else {
                log.error("Trying to update sample not in database, accession: {}", sample.getAccession());
            }

            MongoSample mongoSample = sampleToMongoSampleConverter.convert(sample);
            mongoSample = mongoSampleRepository.save(mongoSample);
            sample = mongoSampleToSampleConverter.convert(mongoSample);

            //send a message for storage and further processing, send relationship targets to identify deleted relationships
            messagingSerivce.fetchThenSendMessage(sample.getAccession(), existingRelationshipTargets);
        } else {
            sample = mongoAccessionService.generateAccession(sample);
            messagingSerivce.fetchThenSendMessage(sample.getAccession());
        }

        //return the sample in case we have modified it i.e accessioned
        //do a fetch to return it with curation objects and inverse relationships
        return fetch(sample.getAccession(), Optional.empty(), null).get();
    }

    public Sample storeSampleStructuredData(Sample newSample) {
        assert newSample.getData() != null;

        if (!(newSample.getData().size() > 0)) {
            throw new SampleValidationException("No structured data is provided");
        }

        if (!newSample.hasAccession()) {
            throw new SampleValidationException("Sample doesn't have an accession");
        } else {
            MongoSample mongoOldSample = mongoSampleRepository.findOne(newSample.getAccession());
            List<String> existingRelationshipTargets = new ArrayList<>();

            if (mongoOldSample != null) {
                Sample oldSample = mongoSampleToSampleConverter.convert(mongoOldSample);
                existingRelationshipTargets = getExistingRelationshipTargets(newSample.getAccession(), mongoOldSample);
                newSample = makeNewSample(newSample, oldSample);
            } else {
                log.error("Trying to update newSample not in database, accession: {}", newSample.getAccession());
            }

            MongoSample mongoSample = sampleToMongoSampleConverter.convert(newSample);
            mongoSample = mongoSampleRepository.save(mongoSample);
            newSample = mongoSampleToSampleConverter.convert(mongoSample);

            //send a message for storage and further processing, send relationship targets to identify deleted relationships
            messagingSerivce.fetchThenSendMessage(newSample.getAccession(), existingRelationshipTargets);
        }

        //return the newSample in case we have modified it i.e accessioned
        //do a fetch to return it with curation objects and inverse relationships
        return fetch(newSample.getAccession(), Optional.empty(), null).get();
    }

    private Sample makeNewSample(Sample newSample, Sample oldSample) {
        return Sample.Builder.fromSample(oldSample).withData(newSample.getData()).withUpdate(Instant.now()).build();
    }

    public boolean searchSampleByDomainAndName(final String domain, final String name) {
        if (mongoSampleRepository.findByDomainAndName(domain, name).size() > 0)
            return true;
        else
            return false;
    }

    public void validateSample(Map sampleAsMap) {
        List<String> errors = sampleValidator.validate(sampleAsMap);
        StringBuilder sb = new StringBuilder();
        if (errors.size() > 0) {
            for (String error : errors) {
                sb.append(error).append("; ");
            }

            throw new SampleValidationException(sb.toString());
        }
    }

    public boolean isExistingAccession(String accession) {
        return mongoSampleRepository.findOne(accession) != null;
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public class SampleValidationException extends RuntimeException {
        private static final long serialVersionUID = -7937033504537036300L;

        public SampleValidationException() {
            super();
        }

        public SampleValidationException(String message, Throwable cause, boolean enableSuppression,
                                         boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }

        public SampleValidationException(String message, Throwable cause) {
            super(message, cause);
        }

        public SampleValidationException(String message) {
            super(message);
        }

        public SampleValidationException(Throwable cause) {
            super(cause);
        }
    }

    private List<String> getExistingRelationshipTargets(String accession, MongoSample mongoOldSample) {
        List<String> oldRelationshipTargets = new ArrayList<>();
        for (MongoRelationship relationship : mongoOldSample.getRelationships()) {
            if (relationship.getSource().equals(accession)) {
                oldRelationshipTargets.add(relationship.getTarget());
            }
        }

        return oldRelationshipTargets;
    }

    private Sample compareWithExistingAndUpdateSample(Sample sampleToUpdate, Sample oldSample) {
        //compare with existing version and check what fields have changed
        if (sampleToUpdate.equals(oldSample)) {
            log.info("New sample is similar to the old sample, accession: {}", oldSample.getAccession());
        }

        //Keep the create date as existing sample -- earlier
        //13/01/2020 - if the sample has a date, acknowledge it. It can be the actual create date from NCBI, ENA.
        return Sample.Builder.fromSample(sampleToUpdate)
                .withCreate(defineCreateDate(sampleToUpdate, oldSample)).build();
    }

    private Instant defineCreateDate(final Sample sampleToUpdate, final Sample oldSample) {
        return (sampleToUpdate.getDomain() != null &&
                sampleToUpdate.getDomain().equalsIgnoreCase("self.BiosampleImportNCBI") &&
                sampleToUpdate.getCreate() != null)
                ? sampleToUpdate.getCreate()
                : (oldSample.getCreate() != null ? oldSample.getCreate() : oldSample.getUpdate());
    }

	/*
	//this code recursively follows relationships
	//TODO finish
	public SortedSet<Sample> getRelated(Sample sample, String relationshipType) {
		Queue<String> toCheck = new LinkedList<>();
		Set<String> checked = new HashSet<>();
		Collection<Sample> related = new TreeSet<>();
		toCheck.add(sample.getAccession());
		while (!toCheck.isEmpty()) {
			String accessionToCheck = toCheck.poll();
			checked.add(accessionToCheck);
			Sample sampleToCheck = sampleReadService.fetchUsing(accessionToCheck);
			related.add(sampleToCheck);
			for (Relationship rel : sampleToCheck.getRelationships()) {
				if (relationshipType == null || relationshipType.equals(rel.getType())) {
					if (!checked.contains(rel.getSource()) && toCheck.contains(rel.getSource())) {
						toCheck.add(rel.getSource());
					}
					if (!checked.contains(rel.getTarget()) && toCheck.contains(rel.getTarget())) {
						toCheck.add(rel.getTarget());
					}
				}
			}
		}
		related.remove(sample);
		return related;
	}
	*/
}
