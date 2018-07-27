package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Iterator;

public class TicketDeserializer extends StdDeserializer<ENAHtsgetTicket> {

    protected TicketDeserializer(Class<?> vc) {
        super(vc);
    }

    protected TicketDeserializer(JavaType valueType) {
        super(valueType);
    }

    protected TicketDeserializer(StdDeserializer<?> src) {
        super(src);
    }

    @Override
    public ENAHtsgetTicket deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ENAHtsgetTicket tikcket = new ENAHtsgetTicket();
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        node = node.get("htsget");
        tikcket.setFormat(node.get("format").asText());
        Iterator<JsonNode> urls =node.get("urls").elements();
        while (urls.hasNext()){
            JsonNode url = urls.next();
            tikcket.addFtpLink(url.get("url").asText());
        }
        tikcket.setMd5Hash(node.get("md5").asText());

        return null;
    }
}
