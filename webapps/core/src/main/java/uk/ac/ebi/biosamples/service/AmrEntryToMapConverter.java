package uk.ac.ebi.biosamples.service;

import org.springframework.core.convert.converter.Converter;
import uk.ac.ebi.biosamples.model.structured.AMREntry;
import uk.ac.ebi.biosamples.model.structured.AMRTable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class AmrEntryToMapConverter implements Converter<AMREntry, Map<String, String>> {
    @Override
    public Map<String, String> convert(AMREntry amrEntry) {
        Map<String, String> amrEntryMap = new HashMap<>();
        Method[] classMethods = AMREntry.class.getDeclaredMethods();
        for (Method m: classMethods) {
            String methodName = m.getName();
            if (methodName.startsWith("get")) {
                try {
                    String value = (String) m.invoke(amrEntry);
                    if (value != null) {
                        String fieldName = getFieldFromMethod(methodName);
                        amrEntryMap.put(fieldName, value);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }

            }
        }

        return amrEntryMap;
    }

    private String getFieldFromMethod(String methodName) {
//        String[] nameParts = methodName.replaceFirst("get", "")
//                .split("[A-Z]");
//
//        return String.join(" ", nameParts);
        return methodName.replaceFirst("get", "");
    }
}
