package uk.ac.ebi.biosamples.service.ga4ghService;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class ObjectUtils {

    public ObjectUtils() {
    }

    public static Map<String, Object> getFieldNamesAndValues(final Object obj, boolean publicOnly)
            throws IllegalArgumentException, IllegalAccessException {
        Class<? extends Object> c1 = obj.getClass();
        Map<String, Object> map = new HashMap<String, Object>();
        Field[] fields = c1.getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName();
            if (publicOnly) {
                if (Modifier.isPublic(field.getModifiers())) {
                    Object value = field.get(obj);
                    map.put(name, value);
                }
            } else {
                field.setAccessible(true);
                Object value = field.get(obj);
                map.put(name, value);
            }
        }
        return map;
    }
}
