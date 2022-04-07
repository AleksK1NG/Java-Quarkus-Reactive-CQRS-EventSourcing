package bankAccount.delivery;


import bankAccount.domain.BankAccountAggregate;
import bankAccount.dto.ChangeAddressRequestDTO;
import bankAccount.dto.ChangeEmailRequestDTO;
import bankAccount.dto.CreateBankAccountRequestDTO;
import bankAccount.dto.DepositAmountRequestDTO;
import es.EventStoreDB;
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
    EventStoreDB eventStoreDB;

    @POST
    public Uni<Response> createBanAccount(@Valid CreateBankAccountRequestDTO dto) {
        final var aggregateID = UUID.randomUUID().toString();
        final var aggregate = new BankAccountAggregate(aggregateID);
        aggregate.createBankAccount(dto.email(), dto.address(), dto.userName());
        logger.infof("aggregate: %s", aggregate);
        return eventStoreDB.save(aggregate).replaceWith(Response.status(201).entity(aggregate).build());
    }

    @POST
    @Path("/email/{aggregateID}")
    public Uni<Response> updateEmail(@PathParam("aggregateID") String aggregateID, @Valid ChangeEmailRequestDTO dto) {
        return eventStoreDB.load(aggregateID, BankAccountAggregate.class)
                .onItem().transform(bankAccountAggregate -> {
                    bankAccountAggregate.changeEmail(dto.newEmail());
                    return bankAccountAggregate;
                })
                .chain(bankAccountAggregate -> eventStoreDB.save(bankAccountAggregate))
                .replaceWith(Response.ok().build());
    }

    @POST
    @Path("/address/{aggregateID}")
    public Uni<Response> changeAddress(@PathParam("aggregateID") String aggregateID, @Valid ChangeAddressRequestDTO dto) {
        return eventStoreDB.load(aggregateID, BankAccountAggregate.class)
                .onItem().transform(bankAccountAggregate -> {
                    bankAccountAggregate.changeAddress(dto.newAddress());
                    return bankAccountAggregate;
                })
                .chain(bankAccountAggregate -> eventStoreDB.save(bankAccountAggregate))
                .replaceWith(Response.ok().build());
    }

    @POST
    @Path("/deposit/{aggregateID}")
    public Uni<Response> depositAmount(@PathParam("aggregateID") String aggregateID, @Valid DepositAmountRequestDTO dto) {
        return eventStoreDB.load(aggregateID, BankAccountAggregate.class)
                .onItem().transform(bankAccountAggregate -> {
                    bankAccountAggregate.depositBalance(dto.amount());
                    return bankAccountAggregate;
                })
                .chain(bankAccountAggregate -> eventStoreDB.save(bankAccountAggregate))
                .replaceWith(Response.ok().build());
    }

    @GET
    @Path("{aggregateID}")
    public Uni<Response> getBanAccount(@PathParam("aggregateID") String aggregateID) {
        return eventStoreDB.load(aggregateID, BankAccountAggregate.class)
                .onItem().invoke(aggregate -> logger.infof("aggregate: %s", aggregate))
                .map(aggregate -> Response.ok(aggregate).build());
    }

}
