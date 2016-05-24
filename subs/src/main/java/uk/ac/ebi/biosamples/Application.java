package uk.ac.ebi.biosamples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

@SpringBootApplication
public class Application {

    /**
     * Provides a Jackson converter for Guava collections objects. Makes it much 
     * easier to use them and should be auto-wired to the relevant places within Spring-boot.
     * 
     * @return
     */
	@Bean
	public Module getGuavaModule() {
	  return new GuavaModule();
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
