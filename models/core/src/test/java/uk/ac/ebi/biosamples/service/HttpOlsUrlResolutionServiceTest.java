package uk.ac.ebi.biosamples.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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