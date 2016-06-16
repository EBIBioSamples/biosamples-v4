package uk.ac.ebi.biosamples.ncbi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;

import uk.ac.ebi.biosamples.utils.XMLFragmenter.ElementCallback;

@Component
public class NCBIFragmentCallback implements ElementCallback {
	
	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private ApplicationContext context;
	
	private LocalDate fromDate;
	private LocalDate toDate;
	private ExecutorService executorService;
	private Queue<Future<Void>> futures;
	
	private NCBIFragmentCallback(){};
	
	public LocalDate getFromDate() {
		return fromDate;
	}

	public void setFromDate(LocalDate fromDate) {
		this.fromDate = fromDate;
	}

	public LocalDate getToDate() {
		return toDate;
	}

	public void setToDate(LocalDate toDate) {
		this.toDate = toDate;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}

	public Queue<Future<Void>> getFutures() {
		return futures;
	}

	public void setFutures(Queue<Future<Void>> futures) {
		this.futures = futures;
	}

	
	@Override
	public void handleElement(Element element) throws InterruptedException, ExecutionException {
		
		log.trace("Handling element");
		
		// have to create multiple beans via context so they all have
		// their own dao object
		// this is apparently bad Inversion Of Control but I can't see a
		// better way to do it
		Callable<Void> callable = context.getBean(NCBIElementCallable.class, element);
		
		if (executorService == null) {
			try {
				callable.call();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			Future<Void> future = executorService.submit(callable);
			if (futures != null) {
				futures.add(future);
			}
		}
	}

	@Override
	public boolean isBlockStart(String uri, String localName, String qName, Attributes attributes) {
		//its not a biosample element, skip
		if (!qName.equals("BioSample")) {
			return false;
		}
		//its not public, skip
		if (!attributes.getValue("", "access").equals("public")) {
			return false;
		}
		//its an EBI biosample, or has no accession, skip
		if (attributes.getValue("", "accession") == null || attributes.getValue("", "accession").startsWith("SAME")) {
			return false;
		}
		//check the date compared to window
		LocalDate updateDate = null;
		updateDate = LocalDate.parse(attributes.getValue("", "last_update"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		LocalDate releaseDate = null;
		releaseDate = LocalDate.parse(attributes.getValue("", "publication_date"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		
		LocalDate latestDate = updateDate;
		if (releaseDate.isAfter(latestDate)) {
			latestDate = releaseDate;
		}
		
		if (fromDate != null && latestDate.isBefore(fromDate)) {
			return false;
		}

		if (toDate != null && latestDate.isAfter(toDate)) {
			return false;
		}
		
		//hasn't failed, so we must be interested in it
		return true;
	}
	

}
