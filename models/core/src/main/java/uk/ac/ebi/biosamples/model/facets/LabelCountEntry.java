package uk.ac.ebi.biosamples.model.facets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Arrays;
import java.util.Map;

@JsonDeserialize(using = LabelCountDeserializer.class)
public class LabelCountEntry implements Comparable<LabelCountEntry>{
    private final String label;
    private final long count;

    private LabelCountEntry(String label, long count) {
        this.label = label;
        this.count = count;
    }

    public String getLabel() {
        return label;
    }

    public long getCount() {
        return count;
    }

    @Override
    public int compareTo(LabelCountEntry o) {
        return Long.compare(this.count, o.count);
    }

    @JsonCreator
    public static LabelCountEntry build(@JsonProperty("label") String label, @JsonProperty("count") long count) {
        if (label == null || label.trim().length() == 0) {
            throw new IllegalArgumentException("label must not be blank");
        }
        return new LabelCountEntry(label.trim(), count);
    }

    @JsonCreator
    public static LabelCountEntry build(Map<String, String> entryMap) {
        if (isValidLabelCount(entryMap)) {
            return new LabelCountEntry(entryMap.get("label"), Long.parseLong(entryMap.get("count")));
        }
        throw new RuntimeException("Provided object is not suitable to be converted to LabelCountEntry");
    }

    public static Boolean isValidLabelCount(Map<String, String> content) {
        return content.keySet().containsAll(Arrays.asList("label", "count"));

    }
}