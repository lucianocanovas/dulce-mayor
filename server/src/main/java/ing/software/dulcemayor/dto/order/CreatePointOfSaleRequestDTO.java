package ing.software.dulcemayor.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePointOfSaleRequestDTO {
    private String storeName;
    private String ownerName;
    private String taxIdOrDni;
    private String phone;
    private String contactEmail;
    private String address;
    private String neighborhood;
    private String city;
    private String deliveryNotes;
    private Double latitude;
    private Double longitude;
    private String salesRepId;
}
