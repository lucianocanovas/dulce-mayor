package ing.software.dulcemayor.service.billing.strategy;

import ing.software.dulcemayor.dto.billing.OrderPaymentStatusDTO;
import ing.software.dulcemayor.dto.order.OrderResponseDTO;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.enums.PaymentType;

/**
 * Strategy pattern abstraction for initiating payment flows, resolving aliases,
 * determining initial order statuses, and enriching DTOs per PaymentType without
 * leaking payment-type conditionals into services, mappers, or PDF generators.
 */
public interface PaymentStrategy {

    boolean supports(PaymentType paymentType);

    boolean matchesAlias(String normalizedAlias);

    PaymentType resolveAliasType(String normalizedAlias);

    OrderStatus getInitialOrderStatus();

    String getDisplayNameEs(PaymentType specificType);

    PaymentInitiationResult initiate(Order order, String returnBaseUrl);

    default void enrichOrderResponse(Order order, OrderResponseDTO.OrderResponseDTOBuilder builder) {
        // default no-op
    }

    default void enrichPaymentStatus(Order order, OrderPaymentStatusDTO.OrderPaymentStatusDTOBuilder builder) {
        // default no-op
    }
}
