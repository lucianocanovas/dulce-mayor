package ing.software.dulcemayor.dto.billing;

import ing.software.dulcemayor.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MercadoPagoQrResponseDTO {
    private String orderId;
    private Long orderNumber;
    private OrderStatus status;
    private double total;
    private String preferenceId;
    private String initPoint;
    private String qrPayload;
    private String qrImageBase64;
}

