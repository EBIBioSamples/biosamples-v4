package uk.ac.ebi.biosamples.model.filters;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

@Service
public class FilterFactory {
    private final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
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

    public Filter parseFilterFromString(String filterString) {
        FilterType filterType = FilterType.ofFilterString(filterString);
        String filterValue = filterString.replace(filterType.getSerialization() + ":","");
        FilterContent filterContent = new EmptyFilter();
        String[] valueElements = filterValue.split(":", 2);
        String filterLabel = valueElements[0];
        if (valueElements.length > 1) {
            filterContent = readContent(filterType, valueElements[1]);
        }
        return new Filter(filterType, filterLabel, filterContent);


    }


    private FilterContent readContent(FilterType type, String serializedFilter) {
        switch(type) {
            case ATTRIBUTE_FILTER:
            case INVERSE_RELATION_FILTER:
            case RELATION_FILER:
                return getValueFilterContent(serializedFilter);
            case DATE_FILTER:
                return getDateFilterContent(serializedFilter);
            default:
                throw new RuntimeException("Unknown filter type");
        }
    }

    private FilterContent getValueFilterContent(String serializedValue) {
        List<String> listContent = new ArrayList<>();
        listContent.add(serializedValue);
        return new ValueFilter(listContent);
    }

    private FilterContent getDateFilterContent(String serializedValue) {
        ZonedDateTime from = null;
        ZonedDateTime to = null;
        int fromIndex = serializedValue.indexOf("from:");
        int toIndex = serializedValue.indexOf("to:");
        if (toIndex != -1) {
            if (fromIndex != -1) {
                from = parseDateTime(serializedValue.substring(fromIndex + 5, toIndex));
            }
            to = parseDateTime(serializedValue.substring(toIndex + 3));
        } else {
            if (fromIndex != -1)
                from = parseDateTime(serializedValue.substring(fromIndex + 5));
        }
        return new DateRangeFilterContent(from, to);
    }


    private ZonedDateTime parseDateTime(String datetime) {
        TemporalAccessor temporalAccessor = formatter.parseBest(datetime,
                ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
        if (temporalAccessor instanceof ZonedDateTime) {
            return (ZonedDateTime) temporalAccessor;
        } else if (temporalAccessor instanceof LocalDateTime) {
            return ((LocalDateTime) temporalAccessor).atZone(ZoneId.of("UTC"));
        } else {
            return ((LocalDate) temporalAccessor).atStartOfDay(ZoneId.of("UTC"));
        }

    }


}
