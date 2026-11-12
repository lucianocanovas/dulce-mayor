package ing.software.dulcemayor.landing.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CustomerProfileDTO implements Serializable {
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
    private String role;
}
