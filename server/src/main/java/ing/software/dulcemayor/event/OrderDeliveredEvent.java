package ing.software.dulcemayor.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Domain event emitted when a delivery route stop is marked as DELIVERED,
 * decoupling post-delivery notifications and next-stop alerts from RouteService.
 */
@Getter
@Builder
public class OrderDeliveredEvent {
    private final String orderId;
    private final Long orderNumber;
    private final String storeName;
    private final String customerName;
    private final String customerEmail;
    private final String deliveryAddress;
    private final String nextStopStoreName;
    private final String nextStopContactEmail;
    private final LocalDateTime deliveredAt;
}
