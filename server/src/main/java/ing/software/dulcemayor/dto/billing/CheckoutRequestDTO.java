package ing.software.dulcemayor.dto.billing;

import ing.software.dulcemayor.dto.order.CreateOrderItemDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequestDTO {
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String taxIdOrDni;
    private String storeName;
    private String deliveryAddress;
    private String deliveryNeighborhood;
    private String deliveryCity;
    private String paymentType;
    private String couponCode;
    private String notes;
    private String returnBaseUrl;
    private List<CreateOrderItemDTO> items;
}

