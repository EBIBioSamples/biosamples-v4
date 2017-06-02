package uk.ac.ebi.biosamples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@EnableHypermediaSupport(type = {EnableHypermediaSupport.HypermediaType.HAL})
public class LegacyJsonApplication {
	public static void main(String[] args) {
		SpringApplication.run(LegacyJsonApplication.class, args);
	}
}
