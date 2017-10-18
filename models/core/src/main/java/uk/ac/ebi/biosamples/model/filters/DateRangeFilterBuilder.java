package uk.ac.ebi.biosamples.model.filters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DateRangeFilterBuilder {

    private final FilterType type = FilterType.DATE_FILTER;
    private final ZoneId defaultZoneId = ZoneId.of("UTC");
    private String label;
    private ZonedDateTime from = LocalDateTime.MIN.atZone(defaultZoneId);
    private ZonedDateTime to = LocalDateTime.MAX.atZone(defaultZoneId);

    public DateRangeFilterBuilder(String label) {
        this.label = label;
    }

    public DateRangeFilterBuilder from(ZonedDateTime fromZonedDateTime) {
        this.from = fromZonedDateTime;
        return this;
    }

    public DateRangeFilterBuilder from(LocalDateTime fromLocalDateTime) {
        this.from = fromLocalDateTime.atZone(defaultZoneId);
        return this;
    }

    public DateRangeFilterBuilder from(LocalDate fromLocalDate) {
        this.from = fromLocalDate.atStartOfDay().atZone(defaultZoneId);
        return this;
    }


    public DateRangeFilterBuilder to(ZonedDateTime toZonedDateTime) {
        this.to = toZonedDateTime;
        return this;
    }

    public DateRangeFilterBuilder to(LocalDateTime toLocalDateTime) {
        this.to = toLocalDateTime.atZone(defaultZoneId);
        return this;
    }

    public DateRangeFilterBuilder to(LocalDate toLocalDate) {
        this.to = toLocalDate.atStartOfDay().atZone(defaultZoneId);
        return this;
    }

    public Filter build() {
        return new Filter(this.type, this.label, new DateRangeFilterContent(this.from, this.to));
    }
}
