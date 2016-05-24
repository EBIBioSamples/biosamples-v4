package uk.ac.ebi.biosamples.webapp;

import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.models.MongoSample;
import uk.ac.ebi.biosamples.models.Sample;
import uk.ac.ebi.biosamples.repositories.MongoSampleRepository;

@RestController
public class SubmissionAPI {

	
	@Autowired
	private MongoSampleRepository mongoSampleRepo;
	
    @RequestMapping(value="/samples", method = RequestMethod.POST)
    public Sample newSample(Sample sample, HttpServletResponse response) throws AlreadyAccessionedException {
    	//check no accession was provided
    	if (sample.getAccession() != null) {
    		throw new AlreadyAccessionedException();
    	}
    	
    	//create an accession
    	String newAccession = getNewAccession();
    	//TODO update sample
    	response.setStatus(HttpServletResponse.SC_CREATED);
    	
    	//get any existing record
    	MongoSample oldSample = null;//mongoSampleRepo.findByAccession(newAccession);
    	//deprecate it
    	if (oldSample != null) {
    		oldSample.doArchive();
    	}
    	
    	//create the new record
    	MongoSample newSample = MongoSample.createFrom(sample);
    	
    	//persist the new sample and the deprecated old sample
    	oldSample = mongoSampleRepo.save(oldSample);
    	newSample = mongoSampleRepo.save(newSample);
    	
    	//TODO put in loading message queue
    	
    	return newSample;
    }
    
    //temporary method, would need to be replaced with real accessioning, however that is done
    private AtomicInteger accessionCount = new AtomicInteger();
    private String getNewAccession() {
    	return "TEST"+accessionCount.getAndIncrement();
    }
    
    @ResponseStatus(value=HttpStatus.BAD_REQUEST, reason="Updates must have an accession")
    public class NotAccessionedException extends Exception {
		private static final long serialVersionUID = 868777054850746361L;
    	
    }
    
    @ResponseStatus(value=HttpStatus.BAD_REQUEST, reason="New samples cannot have an accession")
    public class AlreadyAccessionedException extends Exception {
		private static final long serialVersionUID = 502181687021078545L;
    	
    }
}
