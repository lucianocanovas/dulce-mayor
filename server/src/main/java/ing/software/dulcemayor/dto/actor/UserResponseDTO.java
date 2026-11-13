package ing.software.dulcemayor.dto.actor;

import ing.software.dulcemayor.enums.AuthProvider;
import ing.software.dulcemayor.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private String id;
    private String username;
    private String fullName;
    private String firstName;
    private String lastName;
    private String documentNumber;
    private String employeeIdNumber;
    private String email;
    private UserRole role;
    private AuthProvider authProvider;
    private boolean temporaryPassword;
    private boolean active;
}
