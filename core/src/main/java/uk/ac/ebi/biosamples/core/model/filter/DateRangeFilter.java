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
package uk.ac.ebi.biosamples.core.model.filter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;
import uk.ac.ebi.biosamples.core.model.facet.FacetType;

public class DateRangeFilter implements Filter {

  private final Optional<DateRange> dateRange;
  private final String label;

  DateRangeFilter(final String label, final DateRange dateRange) {
    this.label = label;
    this.dateRange = Optional.ofNullable(dateRange);
  }

  @Override
  public FilterType getType() {
    return FilterType.DATE_FILTER;
  }

  @Override
  public String getLabel() {
    return label;
  }

  @Override
  public FacetType getAssociatedFacetType() {
    return FacetType.NO_TYPE;
  }

  @Override
  public Optional<DateRange> getContent() {
    return dateRange;
  }

  @Override
  public String getSerialization() {
    final StringBuilder serializationBuilder =
        new StringBuilder(getType().getSerialization()).append(":").append(getLabel());

    getContent()
        .ifPresent(
            dateRange -> {
              serializationBuilder.append(":");

              if (!dateRange.isFromMinDate()) {
                serializationBuilder
                    .append("from=")
                    .append(
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                            LocalDateTime.ofInstant(dateRange.getFrom(), ZoneOffset.UTC)));
              }

              if (!dateRange.isUntilMaxDate()) {
                serializationBuilder
                    .append("until=")
                    .append(
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                            LocalDateTime.ofInstant(dateRange.getUntil(), ZoneOffset.UTC)));
              }
            });
    return serializationBuilder.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLabel(), getContent());
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof DateRangeFilter)) {
      return false;
    }
    final DateRangeFilter other = (DateRangeFilter) obj;
    return Objects.equals(getLabel(), other.getLabel())
        && Objects.equals(getContent(), other.getContent());
  }

  public static class DateRangeFilterBuilder implements Filter.Builder {
    private final String label;

    private Instant from = null;
    private Instant until = null;

    public DateRangeFilterBuilder(final String label) {
      this.label = label;
    }

    public DateRangeFilterBuilder from(final Instant from) {
      this.from = from;
      return this;
    }

    public DateRangeFilterBuilder from(final String value) {
      // quick exit
      if (value == null || value.trim().length() == 0) {
        return this;
      }

      try {
        from =
            LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .toInstant(ZoneOffset.UTC);
      } catch (final DateTimeParseException e) {
        // retry for just the date
        from =
            LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
      }
      return this;
    }

    public DateRangeFilterBuilder until(final Instant until) {
      this.until = until;
      return this;
    }

    public DateRangeFilterBuilder until(final String value) {
      // quick exit
      if (value == null || value.trim().length() == 0) {
        return this;
      }

      try {
        until =
            LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .toInstant(ZoneOffset.UTC);
      } catch (final DateTimeParseException e) {
        // retry for just the date
        until =
            LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
                .plusDays(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        // LocalDateTime.ofInstant(this.until, ZoneOffset.UTC).plusDays(1).minusN
      }
      return this;
    }

    @Override
    public DateRangeFilter build() {
      if (from == null && until == null) {
        return new DateRangeFilter(label, null);
      }
      return new DateRangeFilter(label, new DateRange(from, until));
    }

    @Override
    public DateRangeFilterBuilder parseContent(final String filterValue) {
      final String fromValue = extractFromFieldFromString(filterValue);
      final String untilValue = extractToFieldFromString(filterValue);
      return from(fromValue).until(untilValue);
    }

    private String extractFromFieldFromString(final String dateRangeString) {
      final int fromIndex = dateRangeString.indexOf(getFromFieldPrefix());
      final int toIndex = dateRangeString.indexOf(getToFieldPrefix());
      if (fromIndex == -1) {
        return "";
      } else {
        if (toIndex < fromIndex) {
          return dateRangeString.substring(fromIndex + getFromFieldPrefix().length());
        } else {
          return dateRangeString.substring(fromIndex + getFromFieldPrefix().length(), toIndex);
        }
      }
    }

    private String extractToFieldFromString(final String dateRangeString) {
      final int fromIndex = dateRangeString.indexOf(getFromFieldPrefix());
      final int toIndex = dateRangeString.indexOf(getToFieldPrefix());
      if (toIndex == -1) {
        return "";
      } else {
        if (toIndex < fromIndex) {
          return dateRangeString.substring(toIndex + getToFieldPrefix().length(), fromIndex);
        } else {
          return dateRangeString.substring(toIndex + getToFieldPrefix().length());
        }
      }
    }

    private String getFromFieldPrefix() {
      return "from=";
    }

    private String getToFieldPrefix() {
      return "until=";
    }
  }

  public static class DateRange {
    private final Instant from;
    private final Instant until;
    private static final Instant min = LocalDateTime.MIN.atZone(ZoneOffset.UTC).toInstant();
    private static final Instant max = LocalDateTime.MAX.atZone(ZoneOffset.UTC).toInstant();

    private DateRange(final Instant from, final Instant until) {
      if (from == null) {
        this.from = min;
      } else {
        this.from = from;
      }

      if (until == null) {
        this.until = max;
      } else {
        this.until = until;
      }
    }

    public Instant getFrom() {
      return from;
    }

    public Instant getUntil() {
      return until;
    }

    public boolean isFromMinDate() {
      return getFrom().equals(min);
    }

    public boolean isUntilMaxDate() {
      return getUntil().equals(max);
    }

    public static DateRange any() {
      return new DateRange(min, max);
    }

    @Override
    public int hashCode() {
      return Objects.hash(from, until);
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof DateRange)) {
        return false;
      }
      final DateRange other = (DateRange) obj;
      return Objects.equals(from, other.from) && Objects.equals(until, other.until);
    }
  }
}
