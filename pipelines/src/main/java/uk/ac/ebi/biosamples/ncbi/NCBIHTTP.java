package uk.ac.ebi.biosamples.ncbi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.URI;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;

import uk.ac.ebi.biosamples.PipelinesProperties;

@Component
public class NCBIHTTP {

	@Autowired
	private PipelinesProperties pipelinesProperties;

	private Logger log = LoggerFactory.getLogger(getClass());

	private NCBIHTTP() {
	}

	/**
	 * Always downloads the target to a local copy, and then returns 
	 * an InputStream from that local copy for further processing.
	 * 
	 * Note: ensure that this stream is closed in a finally block to prevent
	 * leakage.
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public InputStream streamFromLocalCopy() throws IOException, ParseException {
		
		log.info("Starting streaming fom local copy of NCBI XML");

		URI remoteFileName = pipelinesProperties.getNcbiHttpUri();
		Path tempCopy = Files.createTempFile("biosamples_ncbi", ".tmp");
		Path localCopy = pipelinesProperties.getNCBILocalFile().toPath();

		Files.copy(remoteFileName.toURL().openStream(), tempCopy, StandardCopyOption.REPLACE_EXISTING);

		log.info("Downloaded to temporary location");
		
		Files.move(tempCopy, localCopy, StandardCopyOption.REPLACE_EXISTING);
		

		log.info("Moved to final location");
		
		// now open a stream for the local version
		return new GZIPInputStream(new BufferedInputStream(new FileInputStream(localCopy.toFile())));
	}
	
	public InputStream streamFromRemote() throws IOException, ParseException {
		
		log.info("Starting streaming fom remote of NCBI XML");

		URI remoteFileName = pipelinesProperties.getNcbiHttpUri();
		
		// now open a stream for the local version
		return new GZIPInputStream(new BufferedInputStream(remoteFileName.toURL().openStream()));
	}
}
