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

public class RangeCountListContent extends AbstractList<RangeCountEntry> implements FacetContent {

  private List<RangeCountEntry> rangeCountEntryList;

  @JsonCreator
  public RangeCountListContent(List<RangeCountEntry> rangeCountEntryList) {
    this.rangeCountEntryList = rangeCountEntryList;
  }

  @Override
  public RangeCountEntry get(int index) {
    return rangeCountEntryList.get(index);
  }

  @Override
  public int size() {
    return rangeCountEntryList.size();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("RangeCountListContent(");
    sb.append(rangeCountEntryList);
    sb.append(")");
    return sb.toString();
  }
}
