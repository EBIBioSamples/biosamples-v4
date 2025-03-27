/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.mongo.model;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.ac.ebi.biosamples.model.PipelineAnalytics;
import uk.ac.ebi.biosamples.model.SampleAnalytics;

@Document
public class MongoAnalytics {
  @Id protected String collectionDate;
  private final int year;
  private final int month;
  private final int day;

  protected List<PipelineAnalytics> pipelines;

  @Field("samples")
  private SampleAnalytics sampleAnalytics;

  public MongoAnalytics(
      final String collectionDate, final int year, final int month, final int day) {
    this.collectionDate = collectionDate;
    pipelines = new ArrayList<>();
    this.year = year;
    this.month = month;
    this.day = day;
  }

  public String getCollectionDate() {
    return collectionDate;
  }

  public void setCollectionDate(final String collectionDate) {
    this.collectionDate = collectionDate;
  }

  public List<PipelineAnalytics> getPipelineAnalytics() {
    return pipelines;
  }

  public void addPipelineAnalytics(final PipelineAnalytics pipelineAnalytics) {
    pipelines.add(pipelineAnalytics);
  }

  public SampleAnalytics getSampleAnalytics() {
    return sampleAnalytics;
  }

  public void setSampleAnalytics(final SampleAnalytics sampleAnalytics) {
    this.sampleAnalytics = sampleAnalytics;
  }

  public int getYear() {
    return year;
  }

  public int getMonth() {
    return month;
  }

  public int getDay() {
    return day;
  }

  public List<PipelineAnalytics> getPipelines() {
    return pipelines;
  }
}
