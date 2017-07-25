package uk.ac.ebi.biosamples.solr;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import uk.ac.ebi.biosamples.solr.service.SolrSampleService;

public class CustomLocalDateTimeSolrSerializer extends StdSerializer<LocalDateTime> {

		
	public CustomLocalDateTimeSolrSerializer() {
		this(null);
	}

	public CustomLocalDateTimeSolrSerializer(Class<LocalDateTime> t) {
		super(t);
	}

	@Override
	public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider arg2)
			throws IOException, JsonProcessingException {
		gen.writeString(SolrSampleService.solrFormatter.format(value));
	}
}