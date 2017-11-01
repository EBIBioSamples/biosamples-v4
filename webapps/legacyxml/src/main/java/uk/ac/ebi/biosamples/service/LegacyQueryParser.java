package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filter.DateRangeFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LegacyQueryParser {

    private final String datePattern = "\\d{4}-\\d{2}-\\d{2}";
    private final String dateRangePattern = "\\[(?<from>" + datePattern + ")\\s+TO\\s+(?<until>" + datePattern + ")]";
    private final String typeRangePattern = "(?<type>update|release)date:";
    private final String finalPattern = "(?:" + typeRangePattern + dateRangePattern + ")+";


    public boolean checkQueryContainsDateFilters(String query) {
        return Pattern.compile(finalPattern).matcher(query).find();
    }

    public List<Filter> getDateFiltersFromQuery(String query) {

        List<Filter> dateRangeFilters = new ArrayList<>();

        if (checkQueryContainsDateFilters(query)) {
            Matcher rangeMatcher = Pattern.compile(finalPattern).matcher(query);
            while (rangeMatcher.find()) {
                String type = rangeMatcher.group("type");
                String from = rangeMatcher.group("from");
                String until = rangeMatcher.group("until");

                DateRangeFilter.Builder filterBuilder;
                if (type.equals("update"))
                    filterBuilder = FilterBuilder.create().onUpdateDate();
                else
                    filterBuilder = FilterBuilder.create().onReleaseDate();

                dateRangeFilters.add(filterBuilder.from(from).until(until).build());
            }
        }
        return dateRangeFilters;

    }

    public String cleanQueryFromDateFilters(String query) {
        if (checkQueryContainsDateFilters(query)) {
            query = query.replaceAll(finalPattern, "").trim();
            if (query.isEmpty()) {
                query = "*";
            }
        }
        return query;
    }


}
