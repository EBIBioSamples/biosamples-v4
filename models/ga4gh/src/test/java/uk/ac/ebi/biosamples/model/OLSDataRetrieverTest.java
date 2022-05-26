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
package uk.ac.ebi.biosamples.model;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;
import uk.ac.ebi.biosamples.service.OLSDataRetriever;

public class OLSDataRetrieverTest {
  @Test
  @Ignore
  public void id_retrieving_test() {
    OLSDataRetriever retriever = new OLSDataRetriever();
    retriever.readOntologyJsonFromUrl("http://purl.obolibrary.org/obo/NCBITaxon_9606");
    String expected_id = "NCBITaxon:9606";
    String actual_id = retriever.getOntologyTermId();
    assertEquals(actual_id, expected_id);
  }

  @Test
  @Ignore
  public void label_retreiving_test() {
    OLSDataRetriever retriever = new OLSDataRetriever();
    retriever.readOntologyJsonFromUrl("http://purl.obolibrary.org/obo/NCBITaxon_9606");
    String expected_label = "Homo sapiens";
    String actual_label = retriever.getOntologyTermLabel();
    assertEquals(actual_label, expected_label);
  }
}
