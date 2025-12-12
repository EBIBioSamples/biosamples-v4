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
package uk.ac.ebi.biosamples.service.search;

import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Getter
public class SearchAfterPage<T> extends PageImpl<T> {
  private final Instant update;
  private final String accession;

  public SearchAfterPage(
      List<T> content, Pageable pageable, long total, @NonNull Timestamp update, String accession) {
    super(content, pageable, total);
    this.update = Instant.ofEpochSecond(update.getSeconds(), update.getNanos());
    this.accession = accession;
  }
}
