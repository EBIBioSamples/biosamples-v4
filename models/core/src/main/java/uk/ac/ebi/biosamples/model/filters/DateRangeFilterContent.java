package uk.ac.ebi.biosamples.model.filters;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class DateRangeFilterContent implements FilterContent {

    private DateRange dateRange;

    public DateRangeFilterContent(ZonedDateTime from, ZonedDateTime to) {
        this.dateRange =  DateRange.range(from, to);
    }

    @Override
    public DateRange getContent() {
        return this.dateRange;
    }


//    @Override
//    public void merge(FilterContent otherContent) {
//        if (otherContent instanceof DateRangeFilterContent) {
//            DateRangeFilterContent content = (DateRangeFilterContent) otherContent;
//            DateRange range = content.getContent();
//            ZonedDateTime newFrom = this.dateRange.getFrom();
//            ZonedDateTime newTo = this.dateRange.getTo();
//            if (range.getFrom().isBefore(newFrom)) {
//                newFrom = range.getFrom();
//            }
//
//            if (range.getTo().isAfter(newTo)) {
//                newTo = range.getTo();
//            }
//            this.dateRange = DateRange.range(newFrom, newTo);
//        }
//    }

    @Override
    public String getSerialization() {
        StringBuilder builder = new StringBuilder();
        if (!this.getContent().isFromMinDate()) {
            builder.append("from=").append(this.getContent().getFrom().format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        }
        if (!this.getContent().isToMaxDate()) {
            builder.append("to=").append(this.getContent().getTo().format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        }
        return builder.toString();
    }

    public static class DateRange {
        private ZonedDateTime from;
        private ZonedDateTime to;
        private static final ZoneId defaultZoneId = ZoneId.of("UTC");
        private static final LocalDateTime maxDate = LocalDateTime.MAX;
        private static final LocalDateTime minDate = LocalDateTime.MIN;

        private DateRange(ZonedDateTime from, ZonedDateTime to) {
            this.from = from;
            this.to = to;
        }

        public ZonedDateTime getFrom() {
            return from;
        }

        public ZonedDateTime getTo() {
            return to;
        }

        public boolean isFromMinDate() {
            return this.getFrom().equals(minDate.atZone(defaultZoneId));
        }

        public boolean isToMaxDate() {
            return this.getTo().equals(maxDate.atZone(defaultZoneId));
        }

        public static DateRange range(ZonedDateTime from, ZonedDateTime to) {
            if (from == null && to == null) {
                return DateRange.any();
            } else if (to == null) {
                return new DateRange(from, ZonedDateTime.of(maxDate, defaultZoneId));
            } else if (from == null) {
                return new DateRange(ZonedDateTime.of(minDate, defaultZoneId), to);
            } else {
                return new DateRange(from, to);
            }
        }

        public static DateRange any() {
            return new DateRange(
                    ZonedDateTime.of(minDate, defaultZoneId),
                    ZonedDateTime.of(maxDate, defaultZoneId));
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.dateRange.from, this.dateRange.to);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (! (obj instanceof DateRangeFilterContent)) {
            return false;
        }
        DateRangeFilterContent other = (DateRangeFilterContent) obj;
        return Objects.equals(this.dateRange.from, other.dateRange.from) &&
                Objects.equals(this.dateRange.to, other.dateRange.to);

    }
}
