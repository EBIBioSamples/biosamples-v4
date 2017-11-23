package uk.ac.ebi.biosamples.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mged.magetab.error.ErrorItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonIgnore;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.listener.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.parser.SampleTabParser;
import uk.ac.ebi.arrayexpress2.sampletab.renderer.SampleTabWriter;
import uk.ac.ebi.biosamples.service.ApiKeyService;
import uk.ac.ebi.biosamples.service.SampleTabService;
import uk.ac.ebi.biosamples.service.SampleTabService.AssertingSampleTabOwnershipException;
import uk.ac.ebi.biosamples.service.SampleTabService.ConflictingSampleTabOwnershipException;
import uk.ac.ebi.biosamples.service.SampleTabService.DuplicateDomainSampleException;

@RestController()
public class SampleTabV1Controller {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private SampleTabService sampleTabService;
	@Autowired
	private ApiKeyService apiKeyService;

    @PostMapping(value = "/api/v1/json/va")
    public @ResponseBody Outcome doValidation(@RequestBody SampleTabRequest request) {
    	return parse(request);        
    }
    
    @PostMapping(value = "/api/v1/json/ac")
    public @ResponseBody Outcome doAccession(@RequestBody SampleTabRequest request, @RequestParam(value="apikey") String apiKey) {
    	//handle APIkey
    	if (apiKey == null) {
    		Outcome outcome = getErrorOutcome("API key not present", "API key not present. Contact biosamples@ebi.ac.uk for more information.");
    		return outcome;
    	}
    	Optional<String> domain = apiKeyService.getDomainForApiKey(apiKey);    	
    	if (!domain.isPresent()) {
    		Outcome outcome = getErrorOutcome("API key not recognized", "API key "+apiKey+" was not recognized as a valid API key. Contact biosamples@ebi.ac.uk for more information.");
    		return outcome;
    	}
    	
    	Outcome outcome = parse(request);

        if (outcome.getErrors().size() > 0) {
        	//at least some errors were discovered during parsing
        	return outcome;
        }  else {
        	//no errors, proceed
            //some corrections for hipsci
            if (outcome.sampledata.msi.submissionIdentifier.equals("GCG-HipSci")) {
                outcome.sampledata.msi.submissionIdentifier = "GSB-3";
            }
            // do AAP domain property
            try {
				sampleTabService.accessionSampleTab(outcome.sampledata, "self."+domain.get(), null, true);
			} catch (DuplicateDomainSampleException e) {
				return getErrorOutcome("Unable to accession", e.getMessage()+" Contact biosamples@ebi.ac.uk for more information.");
			}
            return outcome;
        }        
    }
    
    @PostMapping(value = "/api/v1/json/sb")
    public @ResponseBody Outcome doSubmission(@RequestBody SampleTabRequest request,  @RequestParam(value="apikey") String apiKey) {
    	//handle APIkey
    	if (apiKey == null) {
    		Outcome outcome = getErrorOutcome("API key not present", "API key not present. Contact biosamples@ebi.ac.uk for more information.");
    		return outcome;
    	}
    	Optional<String> domain = apiKeyService.getDomainForApiKey(apiKey);    	
    	if (!domain.isPresent()) {
    		Outcome outcome = getErrorOutcome("API key not recognized", "API key "+apiKey+" was not recognized as a valid API key. Contact biosamples@ebi.ac.uk for more information.");
    		return outcome;
    	}    	
    	
    	Outcome outcome = parse(request);

        if (outcome.getErrors().size() > 0) {
        	//at least some errors were discovered during parsing
        	return outcome;
        }  else {
        	//no errors, proceed
            //some corrections for hipsci
            if (outcome.sampledata.msi.submissionIdentifier != null
            		&& outcome.sampledata.msi.submissionIdentifier.equals("GCG-HipSci")) {
                outcome.sampledata.msi.submissionIdentifier = "GSB-3";
            }
            boolean isSuperuser = apiKeyService.getUsernameForApiKey(apiKey).get().equals(ApiKeyService.BIOSAMPLES);
            try {
				sampleTabService.saveSampleTab(outcome.sampledata, "self."+domain.get(), isSuperuser, true);
			} catch (DuplicateDomainSampleException | ConflictingSampleTabOwnershipException | AssertingSampleTabOwnershipException e) {
				log.error("Caught exception "+e.getMessage(), e);
				return getErrorOutcome("Unable to accession", e.getMessage()+" Contact biosamples@ebi.ac.uk for more information.");
			}
            return outcome;
        }        
    }
    
    /*
     * Echoing function. Used for triggering download of javascript
     * processed sampletab files. No way to download a javascript string
     * directly from memory, so it is bounced back off the server through
     * this method.
     */
    @RequestMapping(value = "/api/echo", method = RequestMethod.POST)
    public void echo(String input, HttpServletResponse response) throws IOException {
        //set it to be marked as a download file
        response.setContentType("application/force-download; charset=UTF-8");
        //set the filename to download it as
        response.addHeader("Content-Disposition","attachment; filename=\"sampletab.txt\"");
        response.setHeader("Content-Transfer-Encoding", "binary");

        //writer to the output stream
        //let springs default error handling take over and redirect on error.
        Writer out = null; 
        try {
            out = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
            out.write(input);
        } finally {
            if (out != null){
                try {
                    out.close();
                    response.flushBuffer();
                } catch (IOException e) {
                    //do nothing
                }
            }
        }
        
    }
    
    private Outcome parse(SampleTabRequest request) {
    	
        //setup parser to listen for errors
        SampleTabParser<SampleData> parser = new SampleTabParser<SampleData>();
        
        List<ErrorItem> errorItems;
        errorItems = new ArrayList<ErrorItem>();
        parser.addErrorItemListener(new ErrorItemListener() {
            public void errorOccurred(ErrorItem item) {
                errorItems.add(item);
            }
        });
        SampleData sampledata = null;
        
        InputStream stream = null;
        try {
        	stream = new ByteArrayInputStream(request.asSingleString().getBytes(StandardCharsets.UTF_8));
            //parse the input into sampletab
            //will also validate
            sampledata = parser.parse(stream);
        } catch (ParseException e) {
            //catch parsing errors for malformed submissions
            log.error("parsing error", e);
            return new Outcome(null, e.getErrorItems());
        } finally {
        	if (stream != null) {
        		try {
					stream.close();
				} catch (IOException e) {
					// do nothing
				}
        	}
        }
        return new Outcome(sampledata, errorItems);    	
    }
    

	public static class SampleTabRequest {
	    private List<List<String>> sampletab;
	
	    /**
	     * Default constructor to allow deserialization of JSON into a request bean: present to allow Jackson/spring to
	     * construct a request bean from POST requests properly.
	     */
	    @SuppressWarnings("unused")
	    private SampleTabRequest() {
	    }
	
	    public SampleTabRequest(List<List<String>> sampletab) {
	        this.sampletab = sampletab;
	    }
	
	    public List<List<String>> getSampleTab() {
	        return sampletab;
	    }
	
	    public void setSampletab(List<List<String>> sampletab) {
	        this.sampletab = sampletab;
	    }
	
	    //internal function for combining the list of lists into a tab/newline separated string
	    public String asSingleString() {
	        StringBuilder sb = new StringBuilder(); //this will handle UTF-8 fine
	        boolean firstLine = true;
	        for (List<String> line : sampletab){
	            if (!firstLine){
	                sb.append("\n");
	            }
	            
	            boolean firstCell = true;
	            for(String cell : line){
	                if (!firstCell){
	                    sb.append("\t");
	                }
	                sb.append(cell);
	                firstCell = false;
	            }
	            
	            firstLine = false;
	        }
	        return sb.toString();
	    }
	}

	public static class Outcome {
	
	    private List<Map<String,String>> errors;
	    private List<List<String>> sampletab;
	    private SampleData sampledata;
	    
	    /**
	     * Default constructor to allow deserialization of JSON into a request bean: present to allow Jackson/spring to
	     * construct a request bean from POST requests properly.
	     */
	    public Outcome() {
	        
	    }
	    
	    public Outcome(List<List<String>> sampletab, List<Map<String,String>> errors) {
	        setSampletab(sampletab);
	        setErrors(errors);
	    }
	    
	    public Outcome(SampleData sampledata, Collection<ErrorItem> errorItems) {
	        
	        List<Map<String,String>> errorList = new ArrayList<Map<String,String>>();
	        for (ErrorItem errorItem : errorItems){
	            Map<String, String> errorMap = new HashMap<String, String>();
	            errorMap.put("type", errorItem.getErrorType());
	            errorMap.put("code", new Integer(errorItem.getErrorCode()).toString());
	            errorMap.put("line", new Integer(errorItem.getLine()).toString());
	            errorMap.put("col", new Integer(errorItem.getCol()).toString());
	            errorMap.put("message", errorItem.getMesg());
	            errorMap.put("comment", errorItem.getComment());
	            errorList.add(errorMap);
	        }
	        setErrors(errorList);
	        
	        this.sampledata = sampledata;
	    }
	
	    public List<List<String>> getSampletab() throws IOException {
	        //check for lazy-loading
	        if (sampletab == null && sampledata != null) {
	            //write the sampledata out to a string
	            //then split that string into cells and store
	            StringWriter sw = new StringWriter();
	            SampleTabWriter stw = new SampleTabWriter(sw);
	            
	            stw.write(sampledata);
	            
	            String sampleTabString = sw.toString();
	            List<List<String>> sampleTabListList = new ArrayList<List<String>>();
	            for (String line : sampleTabString.split("\n")){
	                List<String> lineList = new ArrayList<String>();
	                for (String cell : line.split("\t")){
	                    lineList.add(cell);
	                }
	                sampleTabListList.add(lineList);
	            }
	            setSampletab(sampleTabListList);
	            stw.close();
	        }
	        return sampletab;
	    }
	    
	    public void setSampletab(List<List<String>> sampletab) {
	        this.sampletab = sampletab;
	    }
	    
	    public List<Map<String, String>> getErrors() {
	        return errors;
	    }
	    
	    public void setErrors(List<Map<String, String>> errors) {
	        this.errors = errors;
	    }
	}

    protected static Outcome getErrorOutcome(String message, String comment) {
        Outcome o = new Outcome();
        List<Map<String,String>> errorList = new ArrayList<Map<String,String>>();
        Map<String, String> errorMap = new HashMap<String, String>();
        //errorMap.put("type", errorItem.getErrorType());
        //errorMap.put("code", new Integer(errorItem.getErrorCode()).toString());
        //errorMap.put("line", new Integer(errorItem.getLine()).toString());
        //errorMap.put("col", new Integer(errorItem.getCol()).toString());
        errorMap.put("message", message);
        errorMap.put("comment", comment);
        errorList.add(errorMap);
        o.setErrors(errorList);
        return o;
        
    }

}
