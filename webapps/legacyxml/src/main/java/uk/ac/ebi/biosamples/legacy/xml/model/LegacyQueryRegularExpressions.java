package uk.ac.ebi.biosamples.legacy.xml.model;

public enum LegacyQueryRegularExpressions implements RegExpProvider{
    DATE_RANGE_FILTER_QUERY {
        @Override
        public String getPattern() {
            final String datePattern = "\\d{4}-\\d{2}-\\d{2}";
            final String encodedAndDecodedSpaces = "(?:\\s|%20)+";
            final String dateRangePattern = "\\[(?<from>" + datePattern + ")"
                    + encodedAndDecodedSpaces
                    + "TO"
                    + encodedAndDecodedSpaces
                    + "(?<until>" + datePattern + ")]";
            final String typeRangePattern = "(?<type>update|release)date:";
            final String dateRangeFinalPattern = "(?:" + typeRangePattern + dateRangePattern + ")";
//            final String matchesIfAsWholeLine = "^" + dateRangeFinalPattern + "$";
            return dateRangeFinalPattern;
        }
    },

    SAMPLE_ACCESSION_FILTER_QUERY {
        @Override
        public String getPattern() {
            return "(?<accession>SAM[EA|N|E](\\S)*)";
        }
    },

    AND {
        @Override
        public String getPattern() {
            return "(?<and>(?:%20|\\s)*AND(?:%20|\\s)*)?";
        }
    }

}
