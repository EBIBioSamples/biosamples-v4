package uk.ac.ebi.biosamples.model;

import uk.ac.ebi.biosamples.model.filter.DateRangeFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SampleAnalytics {
    private String dateRange;
    private long processedRecords;
    protected Map<String, Long> center;
    protected Map<String, Long> channel;

    public SampleAnalytics() {
        this.center = new HashMap<>();
        this.channel = new HashMap<>();
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

    public long getProcessedRecords() {
        return processedRecords;
    }

    public void setProcessedRecords(long processedRecords) {
        this.processedRecords = processedRecords;
    }

    public Map<String, Long> getCenter() {
        return center;
    }

    public void addToCenter(String centerName) {
        if (center.containsKey(centerName)) {
            center.put(centerName, center.get(centerName) + 1);
        } else {
            center.put(centerName, 1L);
        }
    }

    public Map<String, Long> getChannel() {
        return channel;
    }

    public void addToChannel(String accessionPrefix) {
        if (channel.containsKey(accessionPrefix)) {
            channel.put(accessionPrefix, channel.get(accessionPrefix) + 1);
        } else {
            channel.put(accessionPrefix, 1L);
        }
    }
}
