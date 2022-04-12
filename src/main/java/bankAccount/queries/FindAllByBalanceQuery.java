package bankAccount.queries;

import io.quarkus.panache.common.Page;

public record FindAllByBalanceQuery(Page page) {
}
