package uk.ac.ebi.biosamples.model.filters;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DateRangeFilterContent implements FilterContent {

    private DateRange dateRange;

    public DateRangeFilterContent(ZonedDateTime from, ZonedDateTime to) {
        this.dateRange =  DateRange.range(from, to);
    }

    @Override
    public DateRange getContent() {
        return this.dateRange;
    }

    @Override
    public void merge(FilterContent otherContent) {
        if (otherContent instanceof DateRangeFilterContent) {
            DateRangeFilterContent content = (DateRangeFilterContent) otherContent;
            DateRange range = content.getContent();
            ZonedDateTime newFrom = this.dateRange.getFrom();
            ZonedDateTime newTo = this.dateRange.getTo();
            if (range.getFrom().isBefore(newFrom)) {
                newFrom = range.getFrom();
            }

            if (range.getTo().isAfter(newTo)) {
                newTo = range.getTo();
            }
            this.dateRange = DateRange.range(newFrom, newTo);
        }
    }

    public static class DateRange {
        private ZonedDateTime from;
        private ZonedDateTime to;

        private DateRange(ZonedDateTime form, ZonedDateTime to) {
            this.from = from;
            this.to = to;
        }

        public ZonedDateTime getFrom() {
            return from;
        }

        public ZonedDateTime getTo() {
            return to;
        }

        public static DateRange range(ZonedDateTime from, ZonedDateTime to) {
            if (to.isAfter(from)) {
                return new DateRange(from, to);
            }
            if (from == null && to == null) {
                return DateRange.to(ZonedDateTime.now());
            } else if (from == null){
                return DateRange.to(to);
            } else {
                return DateRange.from(from);
            }
        }

        public static DateRange from(ZonedDateTime from) {
            return new DateRange(from, ZonedDateTime.now());
        }

        public static DateRange to(ZonedDateTime to) {
            return new DateRange(ZonedDateTime.ofInstant(Instant.MIN, to.getZone()), to);
        }

        public static DateRange any() {
            return new DateRange(
                    ZonedDateTime.ofInstant(Instant.MIN, ZoneId.of("UTC")),
                    ZonedDateTime.ofInstant(Instant.MAX, ZoneId.of("UTC")));
        }
    }
}
