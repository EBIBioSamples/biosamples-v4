package uk.ac.ebi.biosamples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class Application {

	public static void main(String[] args) {
		System.exit(SpringApplication.exit(SpringApplication.run(Application.class, args)));
	}
}