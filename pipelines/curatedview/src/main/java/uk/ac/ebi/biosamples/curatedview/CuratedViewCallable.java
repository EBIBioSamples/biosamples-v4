package uk.ac.ebi.biosamples.curatedview;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoSampleStaticViews;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;

import java.util.concurrent.Callable;

public class CuratedViewCallable implements Callable<Void> {
    private final Sample sample;
    private final MongoSampleRepository mongoSampleRepository;
    private final SampleToMongoSampleConverter sampleToMongoSampleConverter;

    CuratedViewCallable(Sample sample, MongoSampleRepository mongoSampleRepository,
                               SampleToMongoSampleConverter sampleToMongoSampleConverter) {
        this.sample = sample;
        this.mongoSampleRepository = mongoSampleRepository;
        this.sampleToMongoSampleConverter = sampleToMongoSampleConverter;
    }

    @Override
    public Void call() {
        persistSamplesToStaticViewCollection();
        return null;
    }

    private void persistSamplesToStaticViewCollection() {
        MongoSample mongoSample = sampleToMongoSampleConverter.convert(sample);
        mongoSampleRepository.insertSample(mongoSample, MongoSampleStaticViews.MONGO_SAMPLE_CURATED);
    }
}
