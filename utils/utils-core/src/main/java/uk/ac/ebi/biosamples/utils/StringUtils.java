package uk.ac.ebi.biosamples.utils;

public class StringUtils {
    public static int nullSafeStringComparison(String str1, String str2) {
        if (str1 == null) {
            return str2 == null ? 0 : -1;
        }
        return str2 == null ? 1 : str1.compareTo(str2);
    }
}
