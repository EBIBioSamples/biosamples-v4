package uk.ac.ebi.biosamples.utils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.UriTemplate;
import org.springframework.web.util.UriUtils;

public class LinkUtils {

	private static Logger log = LoggerFactory.getLogger(LinkUtils.class);


	
	private static String decodeText(String text) {
		if (text != null) {
			try {
				//URLDecoder doesn't work right...
				//text = URLDecoder.decode(text, "UTF-8");
				text = UriUtils.decode(text, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}		
		return text;
	}
	
	public static Link cleanLink(Link link) {
		//expand template to nothing
		link = link.expand(Collections.emptyMap());
		//this won't handle encodings correctly, so need to manually fix that
		link = new Link(decodeText(decodeText(link.getHref())), link.getRel());
		
		return link;
	}
}
