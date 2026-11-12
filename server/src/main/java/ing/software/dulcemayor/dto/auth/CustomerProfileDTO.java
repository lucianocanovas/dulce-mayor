package ing.software.dulcemayor.dto.auth;

import ing.software.dulcemayor.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileDTO {
    private String userId;
    private String retailerId;
    private String email;
    private String fullName;
    private String firstName;
    private String lastName;
    private String phone;
    private String storeName;
    private String taxIdOrDni;
    private String deliveryAddress;
    private String deliveryNeighborhood;
    private String deliveryCity;
    private UserRole role;
}
