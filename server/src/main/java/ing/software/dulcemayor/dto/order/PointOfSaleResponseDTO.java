package ing.software.dulcemayor.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointOfSaleResponseDTO {
    private String id;
    private String storeName;
    private String ownerName;
    private String taxIdOrDni;
    private String phone;
    private String address;
    private String neighborhood;
    private String city;
    private double latitude;
    private double longitude;
    private String salesRepId;
    private String salesRepName;
    private String retailerId;
    private boolean hasActiveOrder;

    public boolean isHasActiveOrder() {
        return hasActiveOrder;
    }

    public boolean getHasActiveOrder() {
        return hasActiveOrder;
    }
}
