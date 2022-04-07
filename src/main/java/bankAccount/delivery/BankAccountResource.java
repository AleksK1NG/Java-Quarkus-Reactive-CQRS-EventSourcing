package bankAccount.delivery;


import bankAccount.commands.*;
import bankAccount.dto.ChangeAddressRequestDTO;
import bankAccount.dto.ChangeEmailRequestDTO;
import bankAccount.dto.CreateBankAccountRequestDTO;
import bankAccount.dto.DepositAmountRequestDTO;
import bankAccount.queries.BankAccountQueryService;
import bankAccount.queries.GetBankAccountByIDQuery;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path(value = "/api/v1/bank")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BankAccountResource {

    @Inject
    Logger logger;

    @Inject
    BankAccountCommandService commandService;

    @Inject
    BankAccountQueryService queryService;


    @POST
    public Uni<Response> createBanAccount(@Valid CreateBankAccountRequestDTO dto) {
        final var aggregateID = UUID.randomUUID().toString();
        final var command = new CreateBankAccountCommand(aggregateID, dto.email(), dto.userName(), dto.address());
        logger.infof("CreateBankAccountCommand: %s", command);
        return commandService.handle(command).map(id -> Response.status(201).entity(id).build());
    }

    @POST
    @Path("/email/{aggregateID}")
    public Uni<Response> updateEmail(@PathParam("aggregateID") String aggregateID, @Valid ChangeEmailRequestDTO dto) {
        final var command = new ChangeEmailCommand(aggregateID, dto.newEmail());
        logger.infof("ChangeEmailCommand: %s", command);
        return commandService.handle(command).map(id -> Response.status(Response.Status.NO_CONTENT).build());
    }

    @POST
    @Path("/address/{aggregateID}")
    public Uni<Response> changeAddress(@PathParam("aggregateID") String aggregateID, @Valid ChangeAddressRequestDTO dto) {
        final var command = new ChangeAddressCommand(aggregateID, dto.newAddress());
        logger.infof("ChangeAddressCommand: %s", command);
        return commandService.handle(command).map(id -> Response.status(Response.Status.NO_CONTENT).build());
    }

    @POST
    @Path("/deposit/{aggregateID}")
    public Uni<Response> depositAmount(@PathParam("aggregateID") String aggregateID, @Valid DepositAmountRequestDTO dto) {
        final var command = new DepositAmountCommand(aggregateID, dto.amount());
        logger.infof("DepositAmountCommand: %s", command);
        return commandService.handle(command).map(id -> Response.status(Response.Status.NO_CONTENT).build());
    }

    @GET
    @Path("{aggregateID}")
    public Uni<Response> getBanAccount(@PathParam("aggregateID") String aggregateID) {
        final var query = new GetBankAccountByIDQuery(aggregateID);
        logger.infof("GetBankAccountByIDQuery: %s", query);
        return queryService.handle(query).onItem().transform(aggregate -> Response.status(200).entity(aggregate).build());
    }

}
