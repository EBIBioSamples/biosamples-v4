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
package uk.ac.ebi.biosamples.mongo.service;

import java.util.function.Function;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.core.model.CurationLink;
import uk.ac.ebi.biosamples.mongo.model.MongoCurationLink;

@Service
public class MongoCurationLinkToCurationLinkConverter
    implements Function<MongoCurationLink, CurationLink> {

  @Override
  public CurationLink apply(final MongoCurationLink mongoCurationLink) {
    return CurationLink.build(
        mongoCurationLink.getSample(),
        mongoCurationLink.getCuration(),
        mongoCurationLink.getDomain(),
        mongoCurationLink.getWebinSubmissionAccountId(),
        mongoCurationLink.getCreated());
  }
}
