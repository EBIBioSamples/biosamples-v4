package uk.ac.ebi.biosamples.legacy.json.domain;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.hateoas.core.Relation;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@Relation(value="sample", collectionRelation = "samples")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(value = {"accession", "name", "releaseDate", "updateDate", "description", "characteristics"})
public class LegacySample {

    @JsonIgnore
    private final uk.ac.ebi.biosamples.model.Sample sample;

    private MultiValueMap<String, LegacyAttribute> characteristics;
    private String description;


    public LegacySample(uk.ac.ebi.biosamples.model.Sample sample) {
        this.sample = sample;
        hydrateLegacySample(sample);
    }

    private void hydrateLegacySample(uk.ac.ebi.biosamples.model.Sample sample) {

        this.description = extractSampleDescription(sample).orElse("");
        this.characteristics = extractCharacteristics(sample);
    }

    private Optional<String> extractSampleDescription(uk.ac.ebi.biosamples.model.Sample sample) {
        Optional<uk.ac.ebi.biosamples.model.Attribute> descriptionAttribute = this.sample.getAttributes().stream()
                .filter(a -> a.getType().equalsIgnoreCase("description"))
                .findAny();
        return descriptionAttribute.map(uk.ac.ebi.biosamples.model.Attribute::getValue);
    }

    private MultiValueMap<String, LegacyAttribute> extractCharacteristics(uk.ac.ebi.biosamples.model.Sample sample) {
        Map<String, List<uk.ac.ebi.biosamples.model.Attribute>> attributesByType = sample.getAttributes()
                .stream()
                .filter(((Predicate<uk.ac.ebi.biosamples.model.Attribute>)this::isDescription).negate())
                .collect(Collectors.groupingBy(uk.ac.ebi.biosamples.model.Attribute::getType));

        MultiValueMap<String, LegacyAttribute> legacyAttributesByType = new LinkedMultiValueMap<>();
        for (String type: attributesByType.keySet()) {
            legacyAttributesByType.put(type,
                    attributesByType.get(type)
                            .stream()
                            .map(LegacyAttribute::new)
                            .collect(Collectors.toList()));
        }
        return legacyAttributesByType;
    }

    @JsonGetter
    public String accession() {
        return this.sample.getAccession();
    }

    @JsonGetter
    public String name() {
        return this.sample.getName();
    }

    @JsonGetter
    public String description() {
        return description;
    }

    @JsonGetter
    @JsonSerialize(using = LocalDateSerializer.class)
    public Instant updateDate() {
        return this.sample.getUpdate();
    }

    @JsonGetter
    @JsonSerialize(using = LocalDateSerializer.class)
    public Instant releaseDate() {
        return this.sample.getRelease();
    }

    @JsonGetter
    public MultiValueMap<String, LegacyAttribute> characteristics() {
        return this.characteristics;
    }


    private boolean isDescription(uk.ac.ebi.biosamples.model.Attribute attribute) {
        return attribute.getType().equalsIgnoreCase("description");
    }


    private static class  LocalDateSerializer extends StdSerializer<Instant> {

        private static final long serialVersionUID = 1L;

        protected LocalDateSerializer() {
            super(Instant.class);
        }


        @Override
        public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
    }
}
