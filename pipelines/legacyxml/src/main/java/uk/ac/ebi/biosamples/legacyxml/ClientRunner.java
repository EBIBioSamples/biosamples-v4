package uk.ac.ebi.biosamples.legacyxml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleToXmlConverter;

@Component
public class ClientRunner implements ApplicationRunner {
	
	private final OutputFormat format = OutputFormat.createCompactFormat();

	private final BioSamplesClient client;
	private final SampleToXmlConverter sampleToXmlConverter;
	private final ObjectMapper objectMapper;
	
	public ClientRunner(BioSamplesClient client, SampleToXmlConverter sampleToXmlConverter, ObjectMapper objectMapper) {
		this.client = client;
		this.sampleToXmlConverter = sampleToXmlConverter;
		this.objectMapper = objectMapper;
	}
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		
		if (!"client".equals(args.getNonOptionArgs().get(0))) {
			return;
		}
		String outputXmlFilename = args.getNonOptionArgs().get(1);
		String outputJsonFilename = args.getNonOptionArgs().get(2);


		XMLWriter xmlWriter = null;
		try (BufferedWriter fileXmlWriter = new BufferedWriter(new FileWriter(new File(outputXmlFilename)));
				BufferedWriter fileJsonWriter = new BufferedWriter(new FileWriter(new File(outputJsonFilename)));) {
			
			fileXmlWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			fileXmlWriter.write("<BioSamples>\n");
			xmlWriter = new XMLWriter(fileXmlWriter, format);
			
			fileJsonWriter.write("[\n");
			ObjectWriter objectWriter = objectMapper.writer();
			boolean first = true;
			
			for (Resource<Sample> resource : client.fetchSampleResourceAll()) {
				Document doc = sampleToXmlConverter.convert(resource.getContent());
				xmlWriter.write(doc.getRootElement());
				
				if (!first) {
					fileJsonWriter.write(",\n");
				} else {
					first = false;
				}
				fileJsonWriter.write(objectWriter.writeValueAsString(resource.getContent()));
			}
			
			fileXmlWriter.write("</BioSamples>\n");
			fileJsonWriter.write("\n]\n");
		} 
	}
}
