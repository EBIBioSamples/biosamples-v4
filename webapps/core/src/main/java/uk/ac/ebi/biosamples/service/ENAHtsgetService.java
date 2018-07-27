package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsunsoft.http.HttpRequest;
import com.jsunsoft.http.ResponseDeserializer;
import com.jsunsoft.http.ResponseHandler;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.ENAHtsgetTicket;
import com.jsunsoft.http.HttpRequestBuilder;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

@Service
public class ENAHtsgetService {

    private final String host = "http://localhost:8080/ga4gh/sample"; //TODO change host to real

    public Optional<ENAHtsgetTicket> getTicket(String accession, String format) {

        HttpRequest<String> request = HttpRequestBuilder.
                createGet(String.format("%s/%s?format=%s", host, accession, format), String.class)
                .responseDeserializer(ResponseDeserializer.ignorableDeserializer())
                .build();
        ResponseHandler<String> responseHandler = request.execute();
        ENAHtsgetTicket ticket = null;
        if (responseHandler.getStatusCode() == HttpStatus.SC_OK) {

            String jsonResponse = responseHandler.get();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = null;
            try {
                node = mapper.readTree(jsonResponse);
            } catch (IOException e) {
                e.printStackTrace();
            }
            node = node.get("htsget");

            ticket = new ENAHtsgetTicket();
            ticket.setAccession(accession);
            ticket.setFormat(format);

            String md5 = node.get("md5Hash").asText();
            ticket.setMd5Hash(md5);

            JsonNode urls = node.get("urls");
            Iterator<JsonNode> urlsIterator = urls.elements();
            while (urlsIterator.hasNext()) {
                JsonNode currentUrl = urlsIterator.next();
                ticket.addFtpLink(currentUrl.get("url").asText());
            }
        }
        return Optional.ofNullable(ticket);


    }


}
