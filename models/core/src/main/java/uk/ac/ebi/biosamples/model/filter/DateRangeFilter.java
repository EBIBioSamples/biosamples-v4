package uk.ac.ebi.biosamples.model.filter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;
import java.util.Optional;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

public class DateRangeFilter implements Filter {

    private DateRange dateRange;
    private String label;

    public DateRangeFilter(String label, DateRange dateRange) {
        this.label = label;
        this.dateRange = dateRange;
    }

    @Override
    public FilterType getType() {
        return FilterType.DATE_FILTER;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public Optional<DateRange> getContent() {
        return Optional.ofNullable(this.dateRange);
    }

    @Override
    public String getSerialization() {
        StringBuilder serializationBuilder = new StringBuilder(this.getType().getSerialization())
                .append(":")
                .append(this.getLabel());

        this.getContent().ifPresent(dateRange -> {

            serializationBuilder.append(":");

            if (!dateRange.isFromMinDate()) {
                serializationBuilder.append("from=").append(dateRange.getFrom().format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
            }

            if (!dateRange.isUntilMaxDate()) {
                serializationBuilder.append("until=").append(dateRange.getUntil().format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
            }

        });
        return serializationBuilder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getLabel(), this.getContent().orElse(null));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (! (obj instanceof DateRangeFilter)) {
            return false;
        }
        DateRangeFilter other = (DateRangeFilter) obj;
        return Objects.equals(this.getLabel(), other.getLabel()) &&
                Objects.equals(this.getContent().orElse(null), other.getContent().orElse(null));

    }

    public static class Builder implements Filter.Builder{
        private final ZoneId defaultZoneId = ZoneId.of("UTC");
        private String label;

        private ZonedDateTime from = null;
        private ZonedDateTime until = null;

        public Builder(String label) {
            this.label = label;
        }

        public Builder from(ZonedDateTime fromZonedDateTime) {
            this.from = fromZonedDateTime;
            return this;
        }

        public Builder from(LocalDateTime fromLocalDateTime) {
            this.from = fromLocalDateTime.atZone(defaultZoneId);
            return this;
        }

        public Builder from(LocalDate fromLocalDate) {
            this.from = fromLocalDate.atStartOfDay().atZone(defaultZoneId);
            return this;
        }

        public Builder from(String stringDate) {
            this.from  = parseDateTime(stringDate, "from");
            return this;
        }


        public Builder until(ZonedDateTime toZonedDateTime) {
            this.until = toZonedDateTime;
            return this;
        }

        public Builder until(LocalDateTime toLocalDateTime) {
            this.until = toLocalDateTime.atZone(defaultZoneId);
            return this;
        }

        public Builder until(LocalDate toLocalDate) {
            this.until = toLocalDate.plusDays(1).atStartOfDay().atZone(defaultZoneId);
            return this;
        }

        public Builder until(String date) {
            this.until = parseDateTime(date, "until");
            return this;
        }

        @Override
        public DateRangeFilter build() {
            if (this.from == null && this.until == null) {
                return new DateRangeFilter(this.label, null);
            }
            return new DateRangeFilter(this.label,
                    new DateRange(from, until));
        }

        @Override
        public Filter.Builder parseContent(String filterValue) {

            String fromString = extractFromFieldFromString(filterValue);
            String toString = extractToFieldFromString(filterValue);

            ZonedDateTime from = parseDateTime(fromString, "from");
            ZonedDateTime until = parseDateTime(toString, "until");
            return this.from(from).until(until);

        }

        private String extractFromFieldFromString(String dateRangeString) {
            int fromIndex = dateRangeString.indexOf(getFromFieldPrefix());
            int toIndex = dateRangeString.indexOf(getToFieldPrefix());
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

        private String extractToFieldFromString(String dateRangeString) {
            int fromIndex = dateRangeString.indexOf(getFromFieldPrefix());
            int toIndex = dateRangeString.indexOf(getToFieldPrefix());
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

        private ZonedDateTime parseDateTime(String datetimeString, String field) {
            if (datetimeString.isEmpty()) return null;
            TemporalAccessor temporalAccessor = getFormatter().parseBest(datetimeString,
                    ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
            if (temporalAccessor instanceof ZonedDateTime) {
                return (ZonedDateTime) temporalAccessor;
            } else if (temporalAccessor instanceof LocalDateTime) {
                return ((LocalDateTime) temporalAccessor).atZone(defaultZoneId);
            } else {
                LocalDate localDate = (LocalDate) temporalAccessor;
                if (field.equalsIgnoreCase("from")) {
                    return localDate.atStartOfDay(defaultZoneId);
                } else {
                    return localDate.plusDays(1).atStartOfDay(defaultZoneId).minusNanos(1);
                }
            }

        }


        private DateTimeFormatter getFormatter() {
            return new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .append(ISO_LOCAL_DATE)
                    .optionalStart()           // time made optional
                    .appendLiteral('T')
                    .append(ISO_LOCAL_TIME)
                    .optionalStart()           // zone and offset made optional
                    .appendOffsetId()
                    .optionalStart()
                    .appendLiteral('[')
                    .parseCaseSensitive()
                    .appendZoneRegionId()
                    .appendLiteral(']')
                    .optionalEnd()
                    .optionalEnd()
                    .optionalEnd()
                    .toFormatter();
        }
    }

    public static class DateRange {
        private ZonedDateTime from;
        private ZonedDateTime until;
        private static final ZoneId defaultZoneId = ZoneId.of("UTC");
        private static final ZonedDateTime maxDate = LocalDateTime.MAX.atZone(defaultZoneId);
        private static final ZonedDateTime minDate = LocalDateTime.MIN.atZone(defaultZoneId);

        private DateRange(ZonedDateTime from, ZonedDateTime until) {
            this.from = Optional.ofNullable(from).orElse(minDate);
            this.until = Optional.ofNullable(until).orElse(maxDate);
        }

        public ZonedDateTime getFrom() {
            return from;
        }

        public ZonedDateTime getUntil() {
            return until;
        }

        public boolean isFromMinDate() {
            return this.getFrom().equals(minDate);
        }

        public boolean isUntilMaxDate() {
            return this.getUntil().equals(maxDate);
        }

        public static DateRange any() {
            return new DateRange(minDate, maxDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.from, this.until);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof DateRange)) {
                return false;
            }
            DateRange other = (DateRange) obj;
            return Objects.equals(this.getFrom(), other.getFrom()) && Objects.equals(this.getUntil(), other.getUntil());

        }
    }

}
