package ing.software.dulcemayor.dto.billing;

import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponseDTO {
    private String orderId;
    private Long orderNumber;
    private OrderStatus status;
    private PaymentType paymentType;
    private double subtotal;
    private double discount;
    private double total;
    private String preferenceId;
    private String initPoint;
    private String qrPayload;
    private String qrImageBase64;
    private String bankAlias;
    private String bankCbu;
    private String bankHolder;
    private String bankName;
    private boolean readyForDispatch;
    private String message;
}

