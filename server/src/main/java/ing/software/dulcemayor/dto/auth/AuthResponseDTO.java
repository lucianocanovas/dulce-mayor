package ing.software.dulcemayor.dto.auth;

import ing.software.dulcemayor.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {
    private String token;
    private String userId;
    private String username;
    private String fullName;
    private UserRole role;
    private boolean temporaryPassword;
}
