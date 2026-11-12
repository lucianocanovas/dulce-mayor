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
    private ing.software.dulcemayor.enums.PaymentType paymentType;
    private String sourceChannel;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String deliveryAddress;
    private String deliveryNeighborhood;
    private String deliveryCity;
    private String mpPreferenceId;
    private String mpInitPoint;
    private String mpPaymentId;
    private String mpPaymentStatus;
    private LocalDateTime paidAt;
    private boolean stockDeducted;
    private boolean readyForDispatch;
    private LocalDateTime dispatchEnabledAt;
    private String invoiceId;
    private Long invoiceNumber;
    private String paymentNote;
    private String qrImageBase64;
    private String bankAlias;
    private String bankCbu;
    private String bankHolder;
}
