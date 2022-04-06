package bankAccount.events;


import bankAccount.domain.BankAccountAggregate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressUpdatedEvent {
    public static final String ADDRESS_UPDATED_V1 = "ADDRESS_UPDATED_V1";
    public static final String AGGREGATE_TYPE = BankAccountAggregate.AGGREGATE_TYPE;

    private String newAddress;

}
