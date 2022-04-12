package filters;

import org.eclipse.microprofile.opentracing.Traced;
import org.jboss.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.util.Locale;

@Provider
@Traced
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private final static Logger logger = Logger.getLogger(LoggingFilter.class);

    @Override
    public void filter(ContainerRequestContext containerRequestContext) {
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) {
        final var method = containerRequestContext.getMethod();
        final var path = containerRequestContext.getUriInfo().getPath();
        final int status = containerResponseContext.getStatus();
        if ( !path.contains("/q/metrics")) {
            logger.infof("(HTTP) method: %s, path: %s, status: %d", method.toUpperCase(Locale.ROOT), path, status);
        }
    }
}
