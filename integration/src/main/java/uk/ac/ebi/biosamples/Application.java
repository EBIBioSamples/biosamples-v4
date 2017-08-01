package uk.ac.ebi.biosamples;

//import org.openqa.grid.internal.utils.configuration.StandaloneConfiguration;
//import org.openqa.selenium.remote.server.SeleniumServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import uk.ac.ebi.biosamples.client.BioSamplesClient;

@SpringBootApplication
public class Application {


	//this is needed to read nonstrings from properties files
	//must be static for lifecycle reasons
	@Bean
	public static PropertySourcesPlaceholderConfigurer getPropertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

//	@Bean
//    public SeleniumServer getSeleniumServer() {
//        return new SeleniumServer(new StandaloneConfiguration());
//    }

	public static void main(String[] args) {
		SpringApplication.exit(SpringApplication.run(Application.class, args));
	}
}
