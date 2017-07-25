package uk.ac.ebi.biosamples;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
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
		
		//generate a large number of samples
		List<Sample> samples = new ArrayList<>();
		for (int i = 0; i < noSamples; i++) {

			Sample sample = generateSample(i, Collections.emptyList());
			samples.add(sample);
		}
		//generate one sample to rule them all
		samples.add(generateSample(noSamples, samples));
		
		//time how long it takes to submit them
		
		long startTime = System.nanoTime();
		client.persistSamples(samples);		
		long endTime = System.nanoTime();
		
		double elapsedMs = (int) ((endTime-startTime)/1000000l);
		double msPerSample = elapsedMs/noSamples;
		log.info("Submitted "+noSamples+" samples in "+elapsedMs+"ms ("+msPerSample+"ms each)");
		if (msPerSample > 15) {
			throw new RuntimeException("Took more than 15ms per sample to submit");
		}

	}

	@Override
	protected void phaseTwo() {
		// time how long it takes to get the highly connected sample
		
		long startTime = System.nanoTime();
		client.fetchSample("SAMbig"+noSamples);
		long endTime = System.nanoTime();
		double elapsedMs = (int) ((endTime-startTime)/1000000l);
		if (elapsedMs > 1000) {
			throw new RuntimeException("Took more than 1000ms to fetch highly-connected sample");
			
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
	
	public Sample generateSample(int i, List<Sample> samples) {

		LocalDateTime update = LocalDateTime.of(LocalDate.of(2016, 5, 5), LocalTime.of(11, 36, 57, 0));
		LocalDateTime release = LocalDateTime.of(LocalDate.of(2016, 4, 1), LocalTime.of(11, 36, 57, 0));
        String domain = null;// "abcde12345";
		
		SortedSet<Attribute> attributes = new TreeSet<>();
		attributes.add(
			Attribute.build("organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

		SortedSet<Relationship> relationships = new TreeSet<>();
		for (Sample other : samples) {
			relationships.add(Relationship.build("SAMbig"+i, "derived from", other.getAccession()));
		}
		
		Sample sample = Sample.build("big sample "+i, "SAMbig"+i, domain, release, update, attributes, relationships, null);

		log.info("built "+sample.getAccession());
		return sample;
	}

}
