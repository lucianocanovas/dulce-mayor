package ing.software.dulcemayor.dto.actor;

import ing.software.dulcemayor.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequestDTO {
    private String username;
    private String firstName;
    private String lastName;
    private String documentNumber;
    private UserRole role;
    private String employeeIdNumber;
    private String password;
}
