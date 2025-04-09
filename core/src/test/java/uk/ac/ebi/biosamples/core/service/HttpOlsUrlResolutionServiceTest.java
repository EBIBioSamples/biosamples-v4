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
package uk.ac.ebi.biosamples.core.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

class HttpOlsUrlResolutionServiceTest {
  @Test
  public void testIsCurie() {
    assertTrue(HttpOlsUrlResolutionService.isCurie("NCBI_1234"));
    assertTrue(HttpOlsUrlResolutionService.isCurie("NCBI:1234"));
    assertFalse(HttpOlsUrlResolutionService.isCurie("N1CBI_1234"));
    assertFalse(HttpOlsUrlResolutionService.isCurie("NCBI_123B4"));
    assertFalse(HttpOlsUrlResolutionService.isCurie("https://test.com/NCBI_1234"));
  }
}
