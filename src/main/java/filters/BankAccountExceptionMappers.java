package filters;

import bankAccount.exceptions.BankAccountNotFoundException;
import exceptions.ExceptionResponseDTO;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;

@ApplicationScoped
public class BankAccountExceptionMappers {

    @Inject
    Logger logger;

    @ServerExceptionMapper(priority = 3)
    public RestResponse<ExceptionResponseDTO> mapRuntimeExceptionException(RuntimeException ex) {
        final var response = new ExceptionResponseDTO(ex.getMessage(), 500, LocalDateTime.now());
        logger.errorf("(mapBankAccountNotFoundException) response: %s", response);
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, response);
    }

    @ServerExceptionMapper(priority = 1)
    public RestResponse<ExceptionResponseDTO> mapBankAccountNotFoundException(BankAccountNotFoundException ex) {
        final var response = new ExceptionResponseDTO(ex.getMessage(), 400, LocalDateTime.now());
        logger.errorf("(mapBankAccountNotFoundException) response: %s", response);
        return RestResponse.status(Response.Status.BAD_REQUEST, response);
    }
}
