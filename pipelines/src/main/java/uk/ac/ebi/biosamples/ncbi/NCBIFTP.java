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
import java.nio.file.Files;
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

import uk.ac.ebi.biosamples.PipelinesProperties;

@Component
public class NCBIFTP {

	@Autowired
	private PipelinesProperties pipelinesProperties;

	private SimpleDateFormat ftpDateTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss");

	private Logger log = LoggerFactory.getLogger(getClass());

	private NCBIFTP() {
	}

	public FTPClient connect() throws SocketException, IOException {
		String server = pipelinesProperties.getNCBIFTPServer();
		FTPClient ftpClient = new FTPClient();
		ftpClient.connect(server);
		ftpClient.login("anonymous", "");
		log.trace("Connected to " + server + ".");
		log.trace(ftpClient.getReplyString());

		// After connection attempt, check the reply code to verify success.
		int reply = ftpClient.getReplyCode();
		if (!FTPReply.isPositiveCompletion(reply)) {
			ftpClient.disconnect();
			throw new IOException("FTP connection failed to complete positively");
		}

		// make sure we are in binary mode
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

		return ftpClient;
	}

	/**
	 * Will check if the local copy exists and is newer than that remote. If
	 * not, will download the remote into the local copy.
	 * 
	 * Then returns an InputStream from that local copy for further processing.
	 * 
	 * Note: ensure that this stream is closed in a finally block to prevent
	 * leakage.
	 * 
	 * Note: the setup() function must have been called first to establish a
	 * connection to the appropriate server.
	 * 
	 * @param remoteFileName
	 * @param localCopy
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public InputStream streamFromLocalCopy() throws IOException, ParseException {

		String remoteFileName = pipelinesProperties.getNCBIRemotePath();
		File localCopy = pipelinesProperties.getNCBILocalFile();
		
		boolean download = true;
		Date ftpModDate = null;
		File localTemp = null;
		FTPClient ftpClient = null;
		
		try {
			ftpClient = connect();
		
			//compare modification date and see if we need to re-download
			String ftpModString = ftpClient.getModificationTime(remoteFileName);
			// ftpModString will be like "213 20150706073959" so need to trim the
			// first part
			ftpModString = ftpModString.split(" ")[1];
			ftpModDate = ftpDateTimeFormat.parse(ftpModString);
	
			if (localCopy.exists()) {
				long modTimeLong = localCopy.lastModified();
				Date modTime = new Date(modTimeLong);
	
				log.trace("FTP time = " + ftpModString);
				log.trace("FTP time = " + ftpModDate);
				log.trace("File time = " + modTimeLong);
				log.trace("File time = " + modTime);
	
				if (modTime.after(ftpModDate)) {
					download = false;
					log.info("Local copy ("+localCopy+") up-to-date, no download needed from "+remoteFileName);
				}
			}
	
			if (download) {
				log.info("Local copy ("+localCopy+") out-of-date, download needed from "+remoteFileName);
	
				// create a local temporary location
				// the move the temporary location to the final one
				localTemp = Files.createTempFile(Paths.get(localCopy.getParentFile().toURI()), "ncbi", "tmp").toFile();
	
				OutputStream fileoutputstream = null;
				try {
					fileoutputstream = new BufferedOutputStream(new FileOutputStream(localTemp));
					ftpClient.retrieveFile(remoteFileName, fileoutputstream);
				} finally {
					if (fileoutputstream != null) {
						try {
							fileoutputstream.close();
						} catch (IOException e) {
							// do nothing
						}
					}
				}
				log.info("Downloaded " + remoteFileName + " to " + localTemp);
				Files.move(localTemp.toPath(), localCopy.toPath(), StandardCopyOption.ATOMIC_MOVE,
						StandardCopyOption.REPLACE_EXISTING);
				log.info("Moved " + localTemp + " to " + localCopy);
			}
		} finally {
			if (ftpClient != null) {
				ftpClient.disconnect();
			}
		}
				


		// now open a stream for the local version
		return new GZIPInputStream(new BufferedInputStream(new FileInputStream(localCopy)));
	}
}
