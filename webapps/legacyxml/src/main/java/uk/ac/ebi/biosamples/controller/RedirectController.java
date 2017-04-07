package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.ac.ebi.biosamples.model.ResultQuery;
import uk.ac.ebi.biosamples.service.SampleService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

@Controller
@RequestMapping("/xml")
public class RedirectController {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Value("${biosamples.submissionuri}")
	private URI biosampleSubmissionUri;

	private SampleService sampleService;

	public RedirectController(SampleService sampleService) {
		this.sampleService = sampleService;
	}

	@RequestMapping(value="/samples/{accession}")
	public void redirectSample(@PathVariable String accession, HttpServletResponse response) throws IOException {
		String redirectUrl = String.format("%s/samples/%s", biosampleSubmissionUri, accession);
		response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML.getType());
		response.sendRedirect(redirectUrl);
	}

	@RequestMapping(value="/groups/{accession:SAMEG\\d+}")
	public void  redirectGroups(@PathVariable String accession, HttpServletResponse response) throws IOException {
		String redirectUrl = String.format("%s/groups/%s", biosampleSubmissionUri, accession);
		response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML.getType());
		response.sendRedirect(redirectUrl);
	}

	@RequestMapping(value = "/samples", produces = {MediaType.TEXT_XML_VALUE})
	public @ResponseBody String getSamples() {
		ResultQuery result = ResultQuery.fromPagedResource(sampleService.getSamples());
	    String xmlDocument =  result.renderDocument();
	    return xmlDocument;
	}
}
