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
package uk.ac.ebi.biosamples.model.facet.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.AbstractList;
import java.util.List;

public class LabelCountListContent extends AbstractList<LabelCountEntry> implements FacetContent {

  private final List<LabelCountEntry> labelCountEntryList;

  @JsonCreator
  public LabelCountListContent(final List<LabelCountEntry> labelCountEntryList) {
    this.labelCountEntryList = labelCountEntryList;
  }

  @Override
  public LabelCountEntry get(final int index) {
    return labelCountEntryList.get(index);
  }

  @Override
  public int size() {
    return labelCountEntryList.size();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("LabelCountListContent(");
    sb.append(labelCountEntryList);
    sb.append(")");
    return sb.toString();
  }
}
