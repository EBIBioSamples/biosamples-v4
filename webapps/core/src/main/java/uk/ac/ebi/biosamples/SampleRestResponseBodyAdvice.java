package uk.ac.ebi.biosamples;

import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import org.springframework.core.MethodParameter;
import org.springframework.hateoas.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import groovy.util.logging.Log;
import uk.ac.ebi.biosamples.controller.SampleRestController;
import uk.ac.ebi.biosamples.model.Sample;

@RestControllerAdvice(assignableTypes = SampleRestController.class)
public class SampleRestResponseBodyAdvice implements ResponseBodyAdvice<Resource<Sample>> {

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return true;
	}

	@Override
	public Resource<Sample> beforeBodyWrite(Resource<Sample> body, MethodParameter returnType,
			MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
			ServerHttpRequest request, ServerHttpResponse response) {
		
		long lastModified = body.getContent().getUpdate().toInstant(ZoneOffset.UTC).toEpochMilli();
		String eTag = "W/\""+body.getContent().hashCode()+"\"";
		
		//an ETag has to be set even on a 304 response see https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.5
		response.getHeaders().setETag(eTag);
		
		if ((request.getHeaders().getIfNoneMatch().contains(eTag) 
				|| request.getHeaders().getIfNoneMatch().contains("*")) 
				|| (request.getHeaders().getIfModifiedSince() != -1 
					&& request.getHeaders().getIfModifiedSince() <= lastModified)
				|| (request.getHeaders().getIfUnmodifiedSince() != -1 
					&& request.getHeaders().getIfUnmodifiedSince() > lastModified)) {
			//the client used a modified cache header that is sufficient to 
			//allow the clients cached copy to be used
			
			//if the request is a get, then use 302
			//if the request is a put, then use 412
			if (request.getMethod().equals(HttpMethod.GET)) {
				response.setStatusCode(HttpStatus.NOT_MODIFIED);
			} else if (request.getMethod().equals(HttpMethod.PUT)) {
				response.setStatusCode(HttpStatus.PRECONDITION_FAILED);
			}
			return null;
		}
		
		
		response.getHeaders().setLastModified(lastModified);
		
		response.getHeaders().setCacheControl(CacheControl.maxAge(1, TimeUnit.MINUTES).cachePublic().getHeaderValue());
		
		return body;
	}

}
