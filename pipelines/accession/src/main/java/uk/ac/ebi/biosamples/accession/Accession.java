package uk.ac.ebi.biosamples.accession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.SubmissionService;

@Component
public class Accession implements ApplicationRunner{

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private AccessionDao accessionDao;
	@Autowired
	private SubmissionService submissionService;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// only do things if we are told to
		if (!args.containsOption("accession")) {
			log.info("skipping accession");
			return;
		}

		log.info("Processing Accession pipeline...");
		
		List<Map<String, Object>> accessions = accessionDao.getAccession();
		
		for (Map<String, Object> accessionMap : accessions) {
			
			for (String key : accessionMap.keySet()) {
				log.info("key "+key+" : "+accessionMap.get(key));
			}
			
			/*
			String name;
			String accession = "SAMEA";
			LocalDateTime release;
			LocalDateTime update;

			SortedSet<Attribute> attributes = new TreeSet<>();
			SortedSet<Relationship> relationships = new TreeSet<>();
			Sample sample = Sample.build(name, accession, release, update, attributes, relationships);
			submissionService.submit(sample);
			*/
		}
	}

}
