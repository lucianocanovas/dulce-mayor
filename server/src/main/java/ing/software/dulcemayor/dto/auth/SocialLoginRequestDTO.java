package ing.software.dulcemayor.dto.auth;

import ing.software.dulcemayor.enums.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginRequestDTO {
    private String email;
    private String name;
    private String sub;
    private AuthProvider provider;
    private String picture;
}
