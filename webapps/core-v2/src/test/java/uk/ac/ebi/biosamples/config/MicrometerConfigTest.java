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
package uk.ac.ebi.biosamples.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MicrometerConfigTest {
  @Test
  void customizeMeterRegistryAddsApplicationAndInstanceTags() throws Exception {
    final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    new MicrometerConfig().customizeMeterRegistry().customize(registry);
    final Counter counter = registry.counter("test.counter");

    assertThat(counter.getId().getTag("application")).isEqualTo("biosamples-webapps-core-v2");
    assertThat(counter.getId().getTag("instance")).isNotBlank();
  }
}
