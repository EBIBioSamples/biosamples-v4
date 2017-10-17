package uk.ac.ebi.biosamples.model.filters;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;

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
        return new ValueFilter(serializedValue);
    }

    private FilterContent getDateFilterContent(String serializedValue) {
        String fromString = extractFromFieldFromString(serializedValue);
        String toString = extractToFieldFromString(serializedValue);

        ZonedDateTime from = parseDateTime(fromString);
        ZonedDateTime to = parseDateTime(toString);
        return new DateRangeFilterContent(from, to);
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

    private ZonedDateTime parseDateTime(String datetimeString) {
        if (datetimeString.isEmpty()) return null;
        TemporalAccessor temporalAccessor = formatter.parseBest(datetimeString,
                ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
        if (temporalAccessor instanceof ZonedDateTime) {
            return (ZonedDateTime) temporalAccessor;
        } else if (temporalAccessor instanceof LocalDateTime) {
            return ((LocalDateTime) temporalAccessor).atZone(ZoneId.of("UTC"));
        } else {
            return ((LocalDate) temporalAccessor).atStartOfDay(ZoneId.of("UTC"));
        }

    }

    private String getFromFieldPrefix() {
        return "from=";
    }

    private String getToFieldPrefix() {
        return "to=";
    }



}
