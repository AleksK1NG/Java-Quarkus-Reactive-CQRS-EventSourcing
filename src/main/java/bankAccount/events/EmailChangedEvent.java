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
public class EmailChangedEvent {
    public static final String EMAIL_CHANGED_V1 = "EMAIL_CHANGED_V1";
    public static final String AGGREGATE_TYPE = BankAccountAggregate.AGGREGATE_TYPE;

    private String newEmail;
}