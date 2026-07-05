package hasara.mlops.filter;

import javax.ws.rs.container.*;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Part 5.5 - API Observability Filter
 *
 * Implements BOTH ContainerRequestFilter and ContainerResponseFilter
 * in one class annotated with @Provider so JAX-RS auto-discovers it.
 *
 * Logs: HTTP method, URI, and final status code for every request/response.
 *
 * Metadata you can extract from contexts (for debugging):
 *   - ContainerRequestContext:  getUriInfo().getRequestUri(), getMethod(),
 *                               getHeaders(), getMediaType()
 *   - ContainerResponseContext: getStatus(), getHeaders(), getMediaType()
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    // Fired BEFORE the resource method executes
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        String uri    = requestContext.getUriInfo().getRequestUri().toString();

        LOGGER.info(String.format("[REQUEST]  %s %s", method, uri));
    }

    // Fired AFTER the resource method executes and a response is ready
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        String method = requestContext.getMethod();
        String uri    = requestContext.getUriInfo().getRequestUri().toString();
        int    status = responseContext.getStatus();

        LOGGER.info(String.format("[RESPONSE] %s %s -> HTTP %d", method, uri, status));
    }
}
