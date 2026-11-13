package ing.software.dulcemayor.dto.auth;

import lombok.Data;

@Data
public class UpdateCustomerProfileRequestDTO {
    private String firstName;
    private String lastName;
    private String phone;
    private String taxIdOrDni;
    private String storeName;
    private String deliveryAddress;
    private String deliveryNeighborhood;
    private String deliveryCity;
}
