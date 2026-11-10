package ing.software.dulcemayor.dto.order;

import ing.software.dulcemayor.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDTO {
    private String id;
    private Long orderNumber;
    private LocalDateTime orderDateTime;
    private double subtotal;
    private double discount;
    private double total;
    private OrderStatus status;
    private String storeName;
    private String retailerName;
    private String salesRepName;
    private String couponCode;
    private int itemCount;
    private java.util.List<OrderDetailResponseDTO> items;
}
