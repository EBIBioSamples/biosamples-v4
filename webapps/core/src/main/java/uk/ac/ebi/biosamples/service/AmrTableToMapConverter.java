package uk.ac.ebi.biosamples.service;

import org.springframework.core.convert.converter.Converter;
import uk.ac.ebi.biosamples.model.structured.amr.AMREntry;
import uk.ac.ebi.biosamples.model.structured.amr.AMRTable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class AmrTableToMapConverter implements Converter<AMRTable, List<Map<String, String>>> {

    @Override
    public List<Map<String, String>> convert(AMRTable amrTable) {

        List<Map<String, String>> amrTableMap = new ArrayList<>();

        for(AMREntry amrEntry: amrTable.getStructuredData()) {
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
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
            amrTableMap.add(amrEntryMap);
        }

        return amrTableMap;

    }

    private String getFieldFromMethod(String methodName) {
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
