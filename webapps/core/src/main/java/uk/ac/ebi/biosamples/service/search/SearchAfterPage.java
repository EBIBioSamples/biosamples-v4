package uk.ac.ebi.biosamples.service.search;

import com.google.protobuf.Timestamp;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

@Getter
public class SearchAfterPage<T> extends PageImpl<T> {
  private final Instant update;
  private final String accession;

  public SearchAfterPage(List<T> content, Pageable pageable, long total, @NonNull Timestamp update, String accession) {
    super(content, pageable, total);
    this.update = Instant.ofEpochSecond(update.getSeconds(), update.getNanos());
    this.accession = accession;
  }
}
