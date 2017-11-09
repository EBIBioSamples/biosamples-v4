package uk.ac.ebi.biosamples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication
public class LegacyJsonApplication extends SpringBootServletInitializer {
	public static void main(String[] args) {
		SpringApplication.run(LegacyJsonApplication.class, args);
	}
}
