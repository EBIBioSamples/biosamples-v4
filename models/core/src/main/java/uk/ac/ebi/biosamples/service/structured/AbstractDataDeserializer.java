package uk.ac.ebi.biosamples.service.structured;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import uk.ac.ebi.biosamples.model.structured.AMREntry;
import uk.ac.ebi.biosamples.model.structured.AMRTable;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.DataType;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

public class AbstractDataDeserializer extends StdDeserializer<AbstractData> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    protected AbstractDataDeserializer() {
        super(AbstractData.class);
    }

    protected AbstractDataDeserializer(Class<AbstractData> t) {
        super(t);
    }

    @Override
    public AbstractData deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

        JsonNode rootNode = p.getCodec().readTree(p);
        URI schema = URI.create(rootNode.get("schema").asText());
        DataType type = DataType.valueOf(rootNode.get("type").asText());
        JsonNode content = rootNode.get("content");

        // Deserialise the object based on the datatype

        // At this point we are sure that the content matches the schema, therefore I can freely take assumptions of
        // what the content look like

        if (type == DataType.AMR) {

            AMRTable.Builder tableBuilder = new AMRTable.Builder(schema);
            for (Iterator<JsonNode> it = content.elements(); it.hasNext(); ) {
                JsonNode amrRowObject = it.next();
                AMREntry entry = this.objectMapper.treeToValue(amrRowObject, AMREntry.class);
                tableBuilder.withEntry(entry);
            }

            return tableBuilder.build();
        }


        return null;
    }
}
