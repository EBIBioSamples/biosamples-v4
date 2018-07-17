package uk.ac.ebi.biosamples.legacyxml;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.XmlGroupToSampleConverter;
import uk.ac.ebi.biosamples.service.XmlSampleToSampleConverter;
import uk.ac.ebi.biosamples.utils.ThreadUtils;
import uk.ac.ebi.biosamples.utils.XmlFragmenter;
import uk.ac.ebi.biosamples.utils.XmlFragmenter.ElementCallback;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

@Component
public class ImportXmlRunner implements ApplicationRunner {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final BioSamplesClient client;
	private final XmlFragmenter xmlFragmenter;
	private final XmlSampleToSampleConverter xmlSampleToSampleConverter;
	private final XmlGroupToSampleConverter xmlGroupToSampleConverter;
	
	private static final String DOMAIN="self.BioSamplesMigration";
	
	public ImportXmlRunner(BioSamplesClient client, XmlFragmenter xmlFragmenter,
						   XmlSampleToSampleConverter xmlSampleToSampleConverter, XmlGroupToSampleConverter xmlGroupToSampleConverter) {
		this.client = client;
		this.xmlFragmenter = xmlFragmenter;
		this.xmlSampleToSampleConverter = xmlSampleToSampleConverter;
		this.xmlGroupToSampleConverter = xmlGroupToSampleConverter;
	}
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		
		if (!"import-xml".equals(args.getNonOptionArgs().get(0))) {
			return;
		}
		
		Path inputXmlPath = Paths.get(args.getNonOptionArgs().get(1));

		try (InputStream is = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(inputXmlPath)))) {

			Map<Element, Future<Resource<Sample>>> futures = new LinkedHashMap<>();

			ElementCallback callback = new ImportElementCallback(futures, 
					client, xmlSampleToSampleConverter, xmlGroupToSampleConverter);

			// this does the actual processing
			xmlFragmenter.handleStream(is, "UTF-8", callback);

			log.info("waiting for futures");

			// wait for anything to finish
			ThreadUtils.checkFutures(futures, 0);
		}
	}
		
	public static class ImportElementCallback implements ElementCallback {

		
		private final Map<Element, Future<Resource<Sample>>> futures;
		private final BioSamplesClient client;
		private final XmlSampleToSampleConverter xmlSampleToSampleConverter;
		private final XmlGroupToSampleConverter xmlGroupToSampleConverter;

		public ImportElementCallback(Map<Element, Future<Resource<Sample>>> futures, BioSamplesClient client, 
				XmlSampleToSampleConverter xmlSampleToSampleConverter, XmlGroupToSampleConverter xmlGroupToSampleConverter) {
			this.futures = futures;
			this.client = client;
			this.xmlSampleToSampleConverter = xmlSampleToSampleConverter;
			this.xmlGroupToSampleConverter = xmlGroupToSampleConverter;
		}
		
		@Override
		public void handleElement(Element e) throws Exception {
			
			Sample sample;
			if (e.getName().equals("BioSample")) {
				sample = xmlSampleToSampleConverter.convert(e);
			} else if (e.getName().equals("BioSampleGroup")) {
				sample = xmlGroupToSampleConverter.convert(e);
			} else {
				return;
			}

			//need to specify domain
//			sample = Sample.build(sample.getName(), sample.getAccession(), DOMAIN,
//					sample.getRelease(), sample.getUpdate(),
//					sample.getAttributes(), sample.getRelationships(), sample.getExternalReferences(),
//					sample.getOrganizations(), sample.getContacts(), sample.getPublications());
            sample = Sample.Builder.fromSample(sample).withDomain(DOMAIN).build();
			
			futures.put(e, client.persistSampleResourceAsync(sample, false, true));

			//make sure we don't have too many futures
			ThreadUtils.checkFutures(futures, 100);
		}

		@Override
		public boolean isBlockStart(String uri, String localName, String qName, Attributes attributes) {
			return qName.equals("BioSample") || qName.equals("BioSampleGroup");
		}
		
	}

}
