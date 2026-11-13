package ing.software.dulcemayor.service.billing.strategy;

import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.enums.PaymentType;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Initiates Cash orders immediately ready for preparation and delivery collection.
 */
@Component
public class CashPaymentStrategy implements PaymentStrategy {

    private static final Set<String> ALIASES = Set.of("EFECTIVO", "CASH");

    @Override
    public boolean supports(PaymentType paymentType) {
        return paymentType == PaymentType.CASH;
    }

    @Override
    public boolean matchesAlias(String normalizedAlias) {
        return normalizedAlias != null && ALIASES.contains(normalizedAlias);
    }

    @Override
    public PaymentType resolveAliasType(String normalizedAlias) {
        return PaymentType.CASH;
    }

    @Override
    public OrderStatus getInitialOrderStatus() {
        return OrderStatus.LOADED;
    }

    @Override
    public String getDisplayNameEs(PaymentType specificType) {
        return "Efectivo contra Entrega";
    }

    @Override
    public PaymentInitiationResult initiate(Order order, String returnBaseUrl) {
        return PaymentInitiationResult.builder()
                .initialOrderStatus(OrderStatus.LOADED)
                .readyForDispatch(true)
                .message("Pedido registrado con pago en efectivo contra entrega.")
                .build();
    }
}
