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

import java.util.ArrayList;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class CurationTest {

  private Logger log = LoggerFactory.getLogger(this.getClass());

  private Curation getCuration() {
    Collection<Attribute> attributePre = new ArrayList<>();
    attributePre.add(Attribute.build("organism", "human"));
    attributePre.add(Attribute.build("taxid", "9606"));
    Collection<Attribute> attributePost = new ArrayList<>();
    attributePost.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    return Curation.build(attributePre, attributePost);
  }

  @Test
  public void testEquality() {
    Curation curation1 = getCuration();
    Curation curation2 = getCuration();

    log.info("curation1 = " + curation1);
    log.info("curation2 = " + curation2);
    log.info("curation1 == curation2 " + curation1.equals(curation2));
    log.info("curation2 == curation1 " + curation2.equals(curation1));

    Assert.assertEquals("curation objects should be equaly", curation1, curation2);
    Assert.assertEquals("curation objects should be equaly", curation2, curation1);
  }
}
