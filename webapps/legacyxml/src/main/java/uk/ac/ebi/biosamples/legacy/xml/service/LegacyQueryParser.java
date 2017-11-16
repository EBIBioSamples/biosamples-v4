package uk.ac.ebi.biosamples.legacy.xml.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filter.DateRangeFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static uk.ac.ebi.biosamples.legacy.xml.model.LegacyQueryRegularExpressions.AND;
import static uk.ac.ebi.biosamples.legacy.xml.model.LegacyQueryRegularExpressions.DATE_RANGE_FILTER_QUERY;
import static uk.ac.ebi.biosamples.legacy.xml.model.LegacyQueryRegularExpressions.SAMPLE_ACCESSION_FILTER_QUERY;

@Service
public class LegacyQueryParser {

    private final String CLEAN_QUERY = "*:*";

    public boolean queryContainsDateRangeFilter(String query) {
        return Pattern.compile(DATE_RANGE_FILTER_QUERY.getPattern()).matcher(query).find();
    }

    public boolean queryContainsSampleFilter(String query) {
        return Pattern.compile(SAMPLE_ACCESSION_FILTER_QUERY.getPattern()).matcher(query).find();
    }

    public Optional<Filter> extractDateFilterFromQuery(String query) {

        Filter dateRangeFilters = null;

        if (queryContainsDateRangeFilter(query)) {
            Matcher rangeMatcher = Pattern.compile(DATE_RANGE_FILTER_QUERY.getPattern()).matcher(query);
            while (rangeMatcher.find()) {
                String type = rangeMatcher.group("type");
                String from = rangeMatcher.group("from");
                String until = rangeMatcher.group("until");

                DateRangeFilter.Builder filterBuilder;
                if (type.equals("update"))
                    filterBuilder = FilterBuilder.create().onUpdateDate();
                else
                    filterBuilder = FilterBuilder.create().onReleaseDate();

                dateRangeFilters = filterBuilder.from(from).until(until).build();
            }
        }
        return Optional.ofNullable(dateRangeFilters);

    }

    public Optional<Filter> extractAccessionFilterFromQuery(String query) {
        Filter accessionFilter = null;

        if (queryContainsSampleFilter(query)) {
            Matcher sampleAccessionMatcher = Pattern.compile(SAMPLE_ACCESSION_FILTER_QUERY.getPattern()).matcher(query);
            while (sampleAccessionMatcher.find()) {
                String accession = sampleAccessionMatcher.group("accession");
                accession = accession.replaceAll("\\*", ".*");
                accessionFilter = FilterBuilder.create().onAccession(accession).build();
            }
        }
        return Optional.ofNullable(accessionFilter);
    }

    public String cleanQueryFromAccessionFilter(String query) {
        if (queryContainsSampleFilter(query)) {
            query = query.replaceAll(AND.getPattern() + SAMPLE_ACCESSION_FILTER_QUERY.getPattern(), "").trim();
            if (query.isEmpty()) {
                query = CLEAN_QUERY;
            }
        }
        return query;
    }

    public String cleanQueryFromDateFilters(String query) {
        if (queryContainsDateRangeFilter(query)) {
            query = query.replaceAll(AND.getPattern() + DATE_RANGE_FILTER_QUERY.getPattern(), "").trim();
            if (query.isEmpty()) {
                query = CLEAN_QUERY;
            }
        }
        return query;
    }


    public String cleanQueryFromKnownFilters(String query) {
        return this.cleanQueryFromAccessionFilter(this.cleanQueryFromDateFilters(query));
    }
}
