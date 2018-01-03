package uk.ac.ebi.biosamples.service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.hateoas.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import uk.ac.ebi.biosamples.controller.SampleRestController;
import uk.ac.ebi.biosamples.model.Sample;

@RestControllerAdvice(assignableTypes = SampleRestController.class)
public class SampleRestResponseBodyAdvice implements ResponseBodyAdvice<Resource<Sample>> {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return returnType.getMethod().getReturnType() == Resource.class;
	}

	@Override
	public Resource<Sample> beforeBodyWrite(Resource<Sample> body, MethodParameter returnType,
			MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
			ServerHttpRequest request, ServerHttpResponse response) {

		//TODO application.properties the cache time

		if (request.getMethod().equals(HttpMethod.GET)) {
			//release date is in future, do not cache it
			if (body.getContent().getRelease().isAfter(Instant.now())) {
				response.getHeaders().setCacheControl(CacheControl.maxAge(1, TimeUnit.MINUTES).cachePrivate().getHeaderValue());
				response.getHeaders().setPragma("no-cache"); //for HTTP/1.0 backwards compatibility
			} else {
				//release date is in the past, public data
				response.getHeaders().setCacheControl(CacheControl.maxAge(1, TimeUnit.MINUTES).cachePublic().getHeaderValue());
			}
		}
		
		//TODO last modified and etag
		return body;
	}

}
