package ing.software.dulcemayor.landing.dto;

import lombok.Data;

@Data
public class PointOfSaleDTO {
    private String id;
    private String storeName;
    private String ownerName;
    private String taxIdOrDni;
    private String phone;
    private String contactEmail;
    private String address;
    private String neighborhood;
    private String city;
    private String deliveryNotes;
}
