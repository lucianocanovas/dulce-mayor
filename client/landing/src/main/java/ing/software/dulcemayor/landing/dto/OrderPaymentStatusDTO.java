package ing.software.dulcemayor.landing.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderPaymentStatusDTO {
    private String orderId;
    private Long orderNumber;
    private String orderStatus;
    private String paymentType;
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
    private List<OrderDetailDTO> items;
    private String message;

    @Data
    public static class OrderDetailDTO {
        private String id;
        private String productId;
        private String productCode;
        private String productName;
        private String packaging;
        private int quantity;
        private double unitPrice;
        private double subtotal;
    }
}

