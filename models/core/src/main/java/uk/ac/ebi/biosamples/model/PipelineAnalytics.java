package uk.ac.ebi.biosamples.model;

import uk.ac.ebi.biosamples.model.filter.DateRangeFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

public class PipelineAnalytics {
    private String name;
    private Instant startTime;
    private Instant endTime;
    private String dateRange;
    private long processedRecords;
    private long modifiedRecords;

    public PipelineAnalytics(String name, Instant startTime, Instant endTime, long processedRecords, long modifiedRecords) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.processedRecords = processedRecords;
        this.modifiedRecords = modifiedRecords;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public String getDateRange() {
        return dateRange;
    }

    public void setDateRange(String dateRange) {
        this.dateRange = dateRange;
    }

    public void setDateRange(Collection<Filter> filters) {
        DateRangeFilter dateRangeFilter = filters.stream()
                .filter(f -> f instanceof DateRangeFilter)
                .map(DateRangeFilter.class::cast)
                .findFirst().orElse(null);
        if (dateRangeFilter != null && dateRangeFilter.getContent().isPresent()) {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
            this.dateRange = dateTimeFormatter.format(dateRangeFilter.getContent().get().getFrom()) +
                    " : " + dateTimeFormatter.format(dateRangeFilter.getContent().get().getUntil());
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getProcessedRecords() {
        return processedRecords;
    }

    public void setProcessedRecords(long processedRecords) {
        this.processedRecords = processedRecords;
    }

    public long getModifiedRecords() {
        return modifiedRecords;
    }

    public void setModifiedRecords(long modifiedRecords) {
        this.modifiedRecords = modifiedRecords;
    }
}
