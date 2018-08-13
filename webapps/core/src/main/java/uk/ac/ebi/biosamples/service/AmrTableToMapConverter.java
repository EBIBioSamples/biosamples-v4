package uk.ac.ebi.biosamples.service;

import org.springframework.core.convert.converter.Converter;
import uk.ac.ebi.biosamples.model.structured.AMREntry;
import uk.ac.ebi.biosamples.model.structured.AMRTable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class AmrTableToMapConverter implements Converter<AMRTable, List<Map<String, String>>> {

    @Override
    public List<Map<String, String>> convert(AMRTable amrTable) {

        List<Map<String, String>> tempMap = new ArrayList<>();
        Set<String> fieldsWithValue = new HashSet<>();

        Set<AMREntry> tableEntries = amrTable.getStructuredData();

        for(AMREntry amrEntry: tableEntries) {
            Map<String, String> amrEntryMap = new HashMap<>();
            Method[] classMethods = AMREntry.class.getDeclaredMethods();
            for (Method m: classMethods) {
                String methodName = m.getName();
                if (methodName.startsWith("get")) {
                    try {
                        String value = (String) m.invoke(amrEntry);
                        if (value != null && !value.isEmpty()) {
                            String fieldName = getFieldFromMethod(methodName);
                            amrEntryMap.put(fieldName, value);
                            fieldsWithValue.add(fieldName);
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
            tempMap.add(amrEntryMap);
        }

        // Clean the table from empty fields
//        return tempMap.parallelStream().peek(entry -> {
//            Set<String>  absentKeys = entry.keySet();
//            absentKeys.removeAll(fieldsWithValue);
//            for(String key: absentKeys) {
//                entry.remove(key);
//            }
//        }).collect(Collectors.toList());
        return tempMap;

    }

    private String getFieldFromMethod(String methodName) {
//        String[] nameParts = methodName.replaceFirst("get", "")
//                .split("[A-Z]");
//
//        return String.join(" ", nameParts);
        return methodName
                .replaceFirst("get", "")
                .replaceAll(
                    String.format("%s|%s|%s",
                            "(?<=[A-Z])(?=[A-Z][a-z])",
                            "(?<=[^A-Z])(?=[A-Z])",
                            "(?<=[A-Za-z])(?=[^A-Za-z])"
                    ),
                    " "
            );
    }

}
