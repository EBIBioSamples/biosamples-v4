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
package uk.ac.ebi.biosamples.solr;

import static org.junit.Assert.*;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.core.service.FilterBuilder;
import uk.ac.ebi.biosamples.solr.model.field.*;
import uk.ac.ebi.biosamples.solr.service.SolrFieldService;
import uk.ac.ebi.biosamples.solr.service.SolrFilterService;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
      SolrSampleAccessionField.class,
      SolrSampleAttributeValueField.class,
      SolrSampleDateField.class,
      SolrSampleExternalReferenceDataField.class,
      SolrSampleInverseRelationField.class,
      SolrSampleNameField.class,
      SolrSampleRelationField.class,
      SolrFilterService.class,
      SolrFieldService.class,
      BioSamplesProperties.class
    })
public class SolrServiceTests {
  @Autowired SolrFilterService solrFilterService;

  @Autowired private SolrFieldService fieldService;

  //    @Before
  //    public void setup() {
  //        this.fieldService = new SolrFieldService(solrSampleFieldList);
  //    }

  @Test
  public void given_encoded_sample_decode_it_correctly_and_of_the_right_type() {
    String encodedField = "MRSXGY3SNFYHI2LPNY_______av_ss";
    String expectedDecodedField = "description";

    SolrSampleField sampleField = fieldService.decodeField(encodedField);
    assertEquals(sampleField.getReadableLabel(), expectedDecodedField);
    assertTrue(sampleField instanceof SolrSampleAttributeValueField);
  }

  @Test
  public void given_fields_with_similar_suffix_return_the_correct_type() {

    SolrSampleField attributeField = fieldService.decodeField("MRSXGY3SNFYHI2LPNY_______av_ss");
    SolrSampleField nameField = fieldService.decodeField("name_s");
    // SolrSampleField domainField = fieldService.decodeField("domain_s");

    assertTrue(attributeField instanceof SolrSampleAttributeValueField);
    assertTrue(nameField instanceof SolrSampleNameField);
  }

  @Test
  public void given_filter_object_return_the_corresponding_solr_field() {
    Filter organismFilter =
        FilterBuilder.create().onAttribute("organism").withValue("Homo sapiens").build();
    SolrSampleAttributeValueField organism = new SolrSampleAttributeValueField("organism");
    Optional<Criteria> criteriaFromField =
        Optional.ofNullable(organism.getFilterCriteria(organismFilter));
    Optional<Criteria> criteriaFromService = solrFilterService.getFilterCriteria(organismFilter);

    assertEquals(criteriaFromField.toString(), criteriaFromService.toString());
  }
}
