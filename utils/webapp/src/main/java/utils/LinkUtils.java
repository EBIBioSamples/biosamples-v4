package utils;

import java.io.UnsupportedEncodingException;
import java.util.Collections;

import org.springframework.hateoas.Link;
import org.springframework.web.util.UriUtils;

public class LinkUtils {


	
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
