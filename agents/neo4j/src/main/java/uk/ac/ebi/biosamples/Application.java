package uk.ac.ebi.biosamples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import uk.ac.ebi.biosamples.messages.threaded.MessageBuffer;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class Application {

	public static void main(String[] args) {
		System.exit(SpringApplication.exit(SpringApplication.run(Application.class, args)));
	}
}
