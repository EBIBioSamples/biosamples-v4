package uk.ac.ebi.biosamples.solr;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.biosamples.solr.model.field.SolrSampleField;

import java.util.List;

@Configuration
//do not use EnableSolrRepositories as it then disables spring boot config
public class SolrConfig {

}
