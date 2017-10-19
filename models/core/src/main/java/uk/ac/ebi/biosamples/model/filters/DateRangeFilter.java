package uk.ac.ebi.biosamples.model.filters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;

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
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public FilterType getKind() {
        return FilterType.DATE_FILTER;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public DateRange getContent() {
        return this.dateRange;
    }

    @Override
    public String getSerialization() {
        StringBuilder dateRangeSerializer = new StringBuilder();
        dateRangeSerializer
                .append(this.getKind().getSerialization())
                .append(":")
                .append(this.getLabel())
                .append(":");

        if (!this.getContent().isFromMinDate()) {
            dateRangeSerializer.append("from=").append(this.getContent().getFrom().format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        }
        if (!this.getContent().isUntilMaxDate()) {
            dateRangeSerializer.append("until=").append(this.getContent().getUntil().format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        }
        return dateRangeSerializer.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.dateRange.getFrom(), this.dateRange.getUntil());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (! (obj instanceof DateRangeFilter)) {
            return false;
        }
        DateRangeFilter other = (DateRangeFilter) obj;
        return Objects.equals(this.dateRange.getFrom(), other.getContent().getFrom()) &&
                Objects.equals(this.dateRange.getUntil(), other.getContent().getUntil());

    }

    public static class Builder implements FilterBuilder{
        private final FilterType type = FilterType.DATE_FILTER;
        private final ZoneId defaultZoneId = ZoneId.of("UTC");
        private String label;
        private ZonedDateTime from = LocalDateTime.MIN.atZone(defaultZoneId);
        private ZonedDateTime to = LocalDateTime.MAX.atZone(defaultZoneId);

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


        public Builder until(ZonedDateTime toZonedDateTime) {
            this.to = toZonedDateTime;
            return this;
        }

        public Builder until(LocalDateTime toLocalDateTime) {
            this.to = toLocalDateTime.atZone(defaultZoneId);
            return this;
        }

        public Builder until(LocalDate toLocalDate) {
            this.to = toLocalDate.atStartOfDay().atZone(defaultZoneId);
            return this;
        }

        @Override
        public DateRangeFilter build() {
            return new DateRangeFilter(this.label, new DateRange(this.from, this.to));
        }

        @Override
        public FilterBuilder parseValue(String filterValue) {

            String fromString = extractFromFieldFromString(filterValue);
            String toString = extractToFieldFromString(filterValue);

            ZonedDateTime from = parseDateTime(fromString);
            ZonedDateTime to = parseDateTime(toString);
            return this.from(from).until(to);

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

        private ZonedDateTime parseDateTime(String datetimeString) {
            if (datetimeString.isEmpty()) return null;
            TemporalAccessor temporalAccessor = getFormatter().parseBest(datetimeString,
                    ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
            if (temporalAccessor instanceof ZonedDateTime) {
                return (ZonedDateTime) temporalAccessor;
            } else if (temporalAccessor instanceof LocalDateTime) {
                return ((LocalDateTime) temporalAccessor).atZone(defaultZoneId);
            } else {
                return ((LocalDate) temporalAccessor).atStartOfDay(defaultZoneId);
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
            this.from = from == null ? minDate : from;
            this.until = until == null ? maxDate : until;
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
    }

}
