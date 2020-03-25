package uk.ac.ebi.biosamples.service.structured;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.DataType;
import uk.ac.ebi.biosamples.model.structured.amr.AMREntry;
import uk.ac.ebi.biosamples.model.structured.amr.AMRTable;

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
    public AbstractData deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode rootNode = p.getCodec().readTree(p);
        URI schema = URI.create(rootNode.get("schema").asText());
        DataType type = DataType.valueOf(rootNode.get("type").asText());
        JsonNode content = rootNode.get("content");
        JsonNode domain = rootNode.get("domain");

        // Deserialise the object based on the datatype

        // At this point we are sure that the content matches the schema, therefore I can freely take assumptions of
        // what the content look like

        if (type == DataType.AMR) {
            AMRTable.Builder tableBuilder = new AMRTable.Builder(schema, domain != null ? domain.asText() : null);

            for (Iterator<JsonNode> it = content.elements(); it.hasNext(); ) {
                JsonNode amrRowObject = it.next();
                AMREntry entry = this.objectMapper.treeToValue(amrRowObject, AMREntry.class);
                tableBuilder.addEntry(entry);
            }

            return tableBuilder.build();
        }

        return null;
    }
}
