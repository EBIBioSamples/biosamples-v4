package uk.ac.ebi.biosamples.neo.repo;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.ogm.session.Session;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.biosamples.neo.model.NeoSample;

public class NeoSampleRepositoryImpl implements NeoSampleRepositoryCustom {


	private Session session;
	
	public NeoSampleRepositoryImpl(Session session) {
		this.session = session;
	}
	
	@Override
	@Transactional
	public NeoSample testNewAccession(String accession) {
		String cypher = "CREATE (s:Sample {accession:'"+accession+"'}) RETURN s";
		Map<String,String> parameters = new HashMap<>();
		//parameters.put("accession", sample.getAccession());
		return session.queryForObject(NeoSample.class, cypher, parameters);
	}

}
