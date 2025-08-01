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
package uk.ac.ebi.biosamples.core.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Test;

public class SampleBuilderTest {

  @Test
  public void sample_build_even_if_null_data_is_provided() {
    final Sample sample = new Sample.Builder("TestSample").withData(null).build();

    assertThat(sample.getName()).isEqualTo("TestSample");
  }
}
