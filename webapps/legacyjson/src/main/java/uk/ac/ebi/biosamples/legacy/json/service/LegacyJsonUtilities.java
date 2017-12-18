package uk.ac.ebi.biosamples.legacy.json.service;

import org.springframework.util.StringUtils;

public class LegacyJsonUtilities {

    public static String camelCaser(String value) {
        StringBuilder finalString = new StringBuilder();
        value = value.replaceAll("[^A-Za-z0-9]"," ").replace("\\s+"," ");
        for(String part: value.split(" ")) {
            part = StringUtils.capitalize(part.toLowerCase());
            if (finalString.length() == 0) {
                finalString.append(part.toLowerCase());
                continue;
            }
            finalString.append(part);
        }
        return finalString.toString();

    }
}
