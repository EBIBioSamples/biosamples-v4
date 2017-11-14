package uk.ac.ebi.biosamples;

import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@Component
@Profile({"big"})
public class BigIntegration extends AbstractIntegration {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	//must be over 1000
	private final int noSamples = 5000;
	
	
	public BigIntegration(BioSamplesClient client) {
		super(client);
	}

	@Override
	protected void phaseOne() {
		
		List<Sample> samples = new ArrayList<>();
		//generate a root sample
		Sample root = generateSample(0, Collections.emptyList(), null);
		samples.add(root);
		//generate a large number of samples
		for (int i = 1; i < noSamples; i++) {

			Sample sample = generateSample(i, Collections.emptyList(), root);
			samples.add(sample);
		}
		//generate one sample to rule them all
		samples.add(generateSample(noSamples, samples, null));
		
		//time how long it takes to submit them
		
		long startTime = System.nanoTime();
		client.persistSamples(samples);		
		long endTime = System.nanoTime();
		
		double elapsedMs = (int) ((endTime-startTime)/1000000l);
		double msPerSample = elapsedMs/noSamples;
		log.info("Submitted "+noSamples+" samples in "+elapsedMs+"ms ("+msPerSample+"ms each)");
		if (msPerSample > 15) {
			throw new RuntimeException("Took more than 15ms per sample to submit ("+msPerSample+"ms each)");
		}

	}

	@Override
	protected void phaseTwo() {
		long startTime;
		long endTime;
		double elapsedMs;
		
		// time how long it takes to get the highly connected sample
		
		startTime = System.nanoTime();
		client.fetchSample("SAMbig"+noSamples);
		endTime = System.nanoTime();
		elapsedMs = (int) ((endTime-startTime)/1000000l);
		if (elapsedMs > 5000) {
			throw new RuntimeException("Took more than 5000ms to fetch highly-connected sample ("+elapsedMs+"ms)");			
		}
		
		startTime = System.nanoTime();
		client.fetchSample("SAMbig"+0);
		endTime = System.nanoTime();
		elapsedMs = (int) ((endTime-startTime)/1000000l);
		if (elapsedMs > 5000) {
			throw new RuntimeException("Took more than 5000ms to fetch highly-connected sample ("+elapsedMs+"ms)");			
		}
		
		//time how long it takes to loop over all of them

		startTime = System.nanoTime();
		for (Resource<Sample> sample : client.fetchSampleResourceAll()) {
			
		}
		endTime = System.nanoTime();
		elapsedMs = (int) ((endTime-startTime)/1000000l);
		if (elapsedMs > 5000) {
			throw new RuntimeException("Took more than 5000ms to fetch all samples ("+elapsedMs+"ms)");			
		}

	}

	@Override
	protected void phaseThree() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void phaseFour() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void phaseFive() {
		// TODO Auto-generated method stub

	}
	
	public Sample generateSample(int i, List<Sample> samples, Sample root) {

		Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
		Instant release = Instant.parse("2016-04-01T11:36:57.00Z");
        String domain = "self.BiosampleIntegrationTest";
		
		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
			Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

		SortedSet<Relationship> relationships = new TreeSet<>();
		for (Sample other : samples) {
			relationships.add(Relationship.build("SAMbig"+i, "derived from", other.getAccession()));
		}
		if (root != null) {
			relationships.add(Relationship.build("SAMbig"+i, "derived from", root.getAccession()));
		}
		
		Sample sample = Sample.build("big sample "+i, "SAMbig"+i, domain, release, update, attributes, relationships, null, null, null);

		log.info("built "+sample.getAccession());
		return sample;
	}

}
