package bankAccount.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public record CreateBankAccountRequestDTO(
        @Email @NotBlank @Size(min = 10, max = 250) String newEmail,
        @NotBlank @Size(min = 10, max = 250) String newAddress,
        @NotBlank @Size(min = 10, max = 250) String userName) {
}
