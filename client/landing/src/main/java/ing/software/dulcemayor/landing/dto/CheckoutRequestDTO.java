package ing.software.dulcemayor.landing.dto;

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
    private List<CartItemDTO> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemDTO {
        private String productId;
        private String productCode;
        private int quantity;
    }
}

