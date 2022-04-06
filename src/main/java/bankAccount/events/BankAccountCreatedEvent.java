package bankAccount.events;

import bankAccount.domain.BankAccountAggregate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BankAccountCreatedEvent {
    public static final String BANK_ACCOUNT_CREATED_V1 = "BANK_ACCOUNT_CREATED_V1";
    public static final String AGGREGATE_TYPE = BankAccountAggregate.AGGREGATE_TYPE;


    private String email;
    private String userName;
    private String address;
}