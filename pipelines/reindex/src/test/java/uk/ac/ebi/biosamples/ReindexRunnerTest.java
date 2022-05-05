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
package uk.ac.ebi.biosamples;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.CloseableIterator;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.utils.mongo.SampleReadService;

@RunWith(MockitoJUnitRunner.class)
public class ReindexRunnerTest {

  @Mock ApplicationArguments applicationArguments;
  @Mock AmqpTemplate amqpTemplate;
  @Mock MongoOperations mongoOperations;
  @Mock SampleReadService sampleReadService;

  private List<String> accessions = Arrays.asList("ACCESSION1", "ACCESSION2", "ACCESSION3");

  private Map<String, Sample> sampleMap = new HashMap<String, Sample>() {};

  @Test
  public void test_log_messages_are_generated_when_samples_not_fetchable() throws Exception {
    CloseableIterator<MongoSample> samples =
        new CloseableIterator<MongoSample>() {
          private int count = 0;
          private final int max = 3;

          @Override
          public void close() {}

          @Override
          public boolean hasNext() {
            return count < max;
          }

          @Override
          public MongoSample next() {
            MongoSample sample = mock(MongoSample.class);
            when(sample.getAccession()).thenReturn("ACCESSION" + (count + 1));
            count++;
            return sample;
          }
        };
    Sample sample1 =
        Sample.build(
            "",
            "ACCESSION1",
            "",
            "",
            Long.valueOf(9606),
            null,
            null,
            null,
            null,
            null,
            null,
            Collections.EMPTY_SET,
            Collections.EMPTY_SET);
    Sample sample3 =
        Sample.build(
            "",
            "ACCESSION3",
            "",
            "",
            Long.valueOf(9606),
            null,
            null,
            null,
            null,
            null,
            null,
            Collections.EMPTY_SET,
            Collections.EMPTY_SET);
    when(mongoOperations.stream(new Query(), MongoSample.class)).thenReturn(samples);
    when(sampleReadService.fetch("ACCESSION1", Optional.empty())).thenReturn(Optional.of(sample1));
    when(sampleReadService.fetch("ACCESSION2", Optional.empty())).thenReturn(Optional.empty());
    when(sampleReadService.fetch("ACCESSION3", Optional.empty()))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(sample3));
    ReindexRunner reindexRunner =
        new ReindexRunner(amqpTemplate, sampleReadService, mongoOperations);
    reindexRunner.run(applicationArguments);
  }
}
