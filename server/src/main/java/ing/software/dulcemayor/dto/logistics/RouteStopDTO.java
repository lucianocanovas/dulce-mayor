package ing.software.dulcemayor.dto.logistics;

import ing.software.dulcemayor.dto.order.PointOfSaleResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteStopDTO {
    private String id;
    private int orderIndex;
    private PointOfSaleResponseDTO pointOfSale;
    private String status;
    private String orderId;
    private Long orderNumber;
    private Double orderTotal;
    private String estimatedArrival;
    private String deliveredAt;
    private String notes;
    private String paymentType;
    private boolean paid;
    private boolean readyForDispatch;
    private Long invoiceNumber;
    private String sourceChannel;
    private String customerName;
    private String mpInitPoint;
    private java.util.List<ing.software.dulcemayor.dto.order.OrderDetailResponseDTO> items;
}
