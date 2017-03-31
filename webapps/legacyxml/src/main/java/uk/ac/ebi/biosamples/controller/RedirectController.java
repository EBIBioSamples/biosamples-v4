package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

@Controller
@RequestMapping("/api/xml")
public class RedirectController {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Value("${biosamples.submissionuri}")
	private URI biosampleSubmissionUri;

	@GetMapping("samples/{accession}")
	public void redirectSample(@PathVariable String accession, HttpServletResponse response) throws IOException {
		String redirectUrl = String.format("%s/samples/%s", biosampleSubmissionUri, accession);
		response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML.getType());
		response.sendRedirect(redirectUrl);
	}
}
