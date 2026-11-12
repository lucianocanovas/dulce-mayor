package ing.software.dulcemayor.landing.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderSummaryDTO {
    private String id;
    private Long orderNumber;
    private LocalDateTime orderDateTime;
    private double subtotal;
    private double discount;
    private double total;
    private String status;
    private String storeName;
    private String customerName;
    private String customerEmail;
    private String deliveryAddress;
    private String deliveryCity;
    private String paymentType;
    private String mpInitPoint;
    private Long invoiceNumber;
    private boolean readyForDispatch;
    private String bankAlias;
    private String bankCbu;
    private String bankHolder;
    private List<OrderPaymentStatusDTO.OrderDetailDTO> items;
}

