package ing.software.dulcemayor.dto.actor;

import ing.software.dulcemayor.enums.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientSummaryDTO {
    private String retailerId;
    private String userId;
    private String username;
    private String fullName;
    private String firstName;
    private String lastName;
    private String documentNumber;
    private String phone;
    private AuthProvider authProvider;
    private boolean hasWebAccount;
    private String storeName;
    private String address;
    private String neighborhood;
    private String city;
    private String salesRepName;
    private int pointsOfSaleCount;
    private boolean active;
}
