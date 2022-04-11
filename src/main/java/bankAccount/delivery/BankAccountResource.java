package bankAccount.delivery;


import bankAccount.commands.*;
import bankAccount.dto.ChangeAddressRequestDTO;
import bankAccount.dto.ChangeEmailRequestDTO;
import bankAccount.dto.CreateBankAccountRequestDTO;
import bankAccount.dto.DepositAmountRequestDTO;
import bankAccount.queries.BankAccountQueryService;
import bankAccount.queries.GetBankAccountByIDQuery;
import exceptions.ExecutionTimeoutException;
import io.opentracing.util.GlobalTracer;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.opentracing.Traced;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Path(value = "/api/v1/bank")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BankAccountResource {
    private final static long EXECUTION_TIMEOUT_SECONDS = 1L;

    @Inject
    Logger logger;

    @Inject
    BankAccountCommandService commandService;

    @Inject
    BankAccountQueryService queryService;


    @POST
    @Traced
    public Uni<Response> createBanAccount(@Valid CreateBankAccountRequestDTO dto) {
        final var aggregateID = UUID.randomUUID().toString();
        final var command = new CreateBankAccountCommand(aggregateID, dto.email(), dto.userName(), dto.address());
        logger.infof("CreateBankAccountCommand: %s", command);
        return commandService.handle(command)
                .onItem().transform(id -> Response.status(Response.Status.CREATED).entity(id).build())
                .ifNoItem().after(Duration.ofSeconds(EXECUTION_TIMEOUT_SECONDS)).failWith(new ExecutionTimeoutException());
    }

    @POST
    @Path("/email/{aggregateID}")
    @Traced
    public Uni<Response> updateEmail(@PathParam("aggregateID") String aggregateID, @Valid ChangeEmailRequestDTO dto) {
        final var command = new ChangeEmailCommand(aggregateID, dto.newEmail());
        logger.infof("ChangeEmailCommand: %s", command);
        return commandService.handle(command)
                .onItem().transform(id -> Response.status(Response.Status.NO_CONTENT).build())
                .ifNoItem().after(Duration.ofSeconds(EXECUTION_TIMEOUT_SECONDS)).failWith(new ExecutionTimeoutException());
    }

    @POST
    @Path("/address/{aggregateID}")
    @Traced
    public Uni<Response> changeAddress(@PathParam("aggregateID") String aggregateID, @Valid ChangeAddressRequestDTO dto) {
        final var command = new ChangeAddressCommand(aggregateID, dto.newAddress());
        logger.infof("ChangeAddressCommand: %s", command);
        return commandService.handle(command)
                .onItem().transform(id -> Response.status(Response.Status.NO_CONTENT).build())
                .ifNoItem().after(Duration.ofSeconds(EXECUTION_TIMEOUT_SECONDS)).failWith(new ExecutionTimeoutException());
    }

    @POST
    @Path("/deposit/{aggregateID}")
    @Traced
    public Uni<Response> depositAmount(@PathParam("aggregateID") String aggregateID, @Valid DepositAmountRequestDTO dto) {
        final var command = new DepositAmountCommand(aggregateID, dto.amount());
        logger.infof("DepositAmountCommand: %s", command);
        return commandService.handle(command)
                .onItem().transform(id -> Response.status(Response.Status.NO_CONTENT).build())
                .ifNoItem().after(Duration.ofSeconds(EXECUTION_TIMEOUT_SECONDS)).failWith(new ExecutionTimeoutException());
    }

    @GET
    @Path("{aggregateID}")
    @Traced
    public Uni<Response> getBanAccount(@PathParam("aggregateID") String aggregateID) {
        Optional.of(GlobalTracer.get().activeSpan()).map(span -> span.setTag("getBanAccount", aggregateID));
        final var query = new GetBankAccountByIDQuery(aggregateID);
        logger.infof("(HTTP getBanAccount) GetBankAccountByIDQuery: %s", query);
        return queryService.handle(query)
                .onItem().transform(aggregate -> Response.status(Response.Status.OK).entity(aggregate).build())
                .ifNoItem().after(Duration.ofSeconds(EXECUTION_TIMEOUT_SECONDS)).failWith(new ExecutionTimeoutException());
    }

}
