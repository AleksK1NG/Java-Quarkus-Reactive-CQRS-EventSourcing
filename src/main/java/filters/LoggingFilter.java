package filters;

import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

//@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Inject
    Logger logger;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        String method = containerRequestContext.getMethod();
        String path = containerRequestContext.getUriInfo().getPath();
        int status = containerResponseContext.getStatus();
        logger.infof("(LoggingFilter) method: %s, path: %s, status: %d", method, path, status);
    }
}
