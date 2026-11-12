package ing.software.dulcemayor.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRegisterRequestDTO {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phone;
    private String storeName;
    private String taxIdOrDni;
    private String deliveryAddress;
    private String deliveryNeighborhood;
    private String deliveryCity;
}
