package uk.ac.ebi.biosamples.ena;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.PipelinesProperties;

@Component
public class ENA implements ApplicationRunner {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private ERAproDAO eraprodao;
	
	@Autowired
	private PipelinesProperties pipelinesProperties;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// only do things if we are told to
		if (!args.containsOption("ena")) {
			return;
		}

		log.info("Processing ENA pipeline...");

		LocalDate fromDate = null;
		if (args.getOptionNames().contains("from")) {
			fromDate = LocalDate.parse(args.getOptionValues("from").iterator().next(),
					DateTimeFormatter.ISO_LOCAL_DATE);
		}
		LocalDate toDate = null;
		if (args.getOptionNames().contains("until")) {
			toDate = LocalDate.parse(args.getOptionValues("until").iterator().next(), DateTimeFormatter.ISO_LOCAL_DATE);
		}
	}

}
