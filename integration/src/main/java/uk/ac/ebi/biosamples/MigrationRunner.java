package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Profile({"default", "migration"})
public class MigrationRunner implements ApplicationRunner, ExitCodeGenerator {

	private BioSamplesClient bioSamplesClient;
	
	private int exitCode = 1;
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	public MigrationRunner(BioSamplesClient bioSamplesClient) {
		this.bioSamplesClient = bioSamplesClient;
	}
	
	@Override
	public int getExitCode() {
		return exitCode;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Starting MigrationRunner");

		if (args.containsOption("migration")) {
			//get the accessions from a file
			List<String> accessions = new ArrayList<>();
			ClassPathResource cpr = new ClassPathResource("/bulkaccession.txt");
			try (BufferedReader br = new BufferedReader(new InputStreamReader(cpr.getInputStream()))) {
				String line;
				while ((line = br.readLine()) != null) {
					accessions.add(line.trim());
				}
			}
			
			//pick a random subset
			Collections.shuffle(accessions);
			accessions = accessions.subList(0, 10000);
			
			//now query to get each of the accessions and time how long it takes
			long startTime = System.nanoTime();
						
			for (Resource<Sample> resource : bioSamplesClient.fetchResourceAll(accessions) ){
				String acc = resource.getContent().getAccession();
				log.trace(acc);
			}
			
			long endTime = System.nanoTime();
			long elapsedTime = endTime-startTime;
			long nsPerSample = elapsedTime/accessions.size();
			double msPerSample = (double)nsPerSample / 1000000.0;
			
			log.info("Fetched "+accessions.size()+" at "+msPerSample+" ms each");
			
		}
		exitCode = 0;
		log.info("Finished MigrationRunner");
	}

}
