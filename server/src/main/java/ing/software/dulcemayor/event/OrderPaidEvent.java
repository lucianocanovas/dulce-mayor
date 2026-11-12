package ing.software.dulcemayor.event;

import ing.software.dulcemayor.enums.PaymentType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Domain event emitted once when an order payment is accredited.
 */
@Getter
@Builder
public class OrderPaidEvent {
    private final String orderId;
    private final Long orderNumber;
    private final Long invoiceNumber;
    private final PaymentType paymentType;
    private final double total;
    private final String customerName;
    private final String customerEmail;
    private final String storeName;
    private final String deliveryAddress;
    private final String paymentReference;
    private final LocalDateTime paidAt;
}

