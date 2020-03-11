package uk.ac.ebi.biosamples.mongo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.ac.ebi.biosamples.model.PipelineAnalytics;
import uk.ac.ebi.biosamples.model.SampleAnalytics;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "analytics")
public class MongoAnalytics {
    @Id
    protected String collectionDate;
    protected List<PipelineAnalytics> pipelines;
    @Field("samples")
    protected SampleAnalytics sampleAnalytics;

    public MongoAnalytics(String collectionDate) {
        this.collectionDate = collectionDate;
        pipelines = new ArrayList<>();
    }

    public String getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(String collectionDate) {
        this.collectionDate = collectionDate;
    }

    public List<PipelineAnalytics> getPipelineAnalytics() {
        return pipelines;
    }

    public void addPipelineAnalytics(PipelineAnalytics pipelineAnalytics) {
        this.pipelines.add(pipelineAnalytics);
    }

    public SampleAnalytics getSampleAnalytics() {
        return sampleAnalytics;
    }

    public void setSampleAnalytics(SampleAnalytics sampleAnalytics) {
        this.sampleAnalytics = sampleAnalytics;
    }
}
