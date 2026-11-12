package ing.software.dulcemayor.landing.dto;

import lombok.Data;

@Data
public class AuthResponseDTO {
    private String token;
    private String userId;
    private String username;
    private String fullName;
    private String role;
    private boolean temporaryPassword;
}
