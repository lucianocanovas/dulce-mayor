package ing.software.dulcemayor.dto.billing;

import ing.software.dulcemayor.dto.order.OrderDetailResponseDTO;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentStatusDTO {
    private String orderId;
    private Long orderNumber;
    private OrderStatus orderStatus;
    private PaymentType paymentType;
    private boolean paid;
    private boolean readyForDispatch;
    private boolean stockDeducted;
    private double subtotal;
    private double discount;
    private double total;
    private String mpPreferenceId;
    private String mpInitPoint;
    private String mpPaymentId;
    private String mpPaymentStatus;
    private LocalDateTime paidAt;
    private String invoiceId;
    private Long invoiceNumber;
    private String storeName;
    private String customerName;
    private String customerEmail;
    private String deliveryAddress;
    private String deliveryCity;
    private String bankAlias;
    private String bankCbu;
    private String bankHolder;
    private String bankName;
    private List<OrderDetailResponseDTO> items;
    private String message;
}

