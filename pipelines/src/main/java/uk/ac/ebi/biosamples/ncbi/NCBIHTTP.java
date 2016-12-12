package uk.ac.ebi.biosamples.ncbi;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.PipelinesProperties;

@Component
public class NCBIHTTP {

	@Autowired
	private PipelinesProperties pipelinesProperties;

	private Logger log = LoggerFactory.getLogger(getClass());

	private NCBIHTTP() {
	}
	
	public InputStream streamFromRemote() throws IOException, ParseException {
		
		log.info("Starting streaming fom remote of NCBI XML");

		URI remoteFileName = pipelinesProperties.getNCBIHttpUri();
		
		// now open a stream for the local version
		return new GZIPInputStream(new BufferedInputStream(remoteFileName.toURL().openStream()));
	}
}
