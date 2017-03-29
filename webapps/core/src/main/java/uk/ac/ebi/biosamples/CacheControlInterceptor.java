package uk.ac.ebi.biosamples;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interceptor to add Cache-Control headers to GET requests
 * 
 * Not useful for spring data rest repositories that use @LastModified annotations
 * 
 * @author faulcon
 *
 */
public class CacheControlInterceptor implements HandlerInterceptor {
	@SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		//only cache get requests
//		if (request.getMethod().equals("GET")) {
			//allow response to be cached for one hour
//			response.addHeader(HttpHeaders.CACHE_CONTROL, "max-age=3600");
//		}
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) throws Exception {
		// do nothing
		
	}
}
