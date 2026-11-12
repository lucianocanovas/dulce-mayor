package ing.software.dulcemayor.service.billing.strategy;

import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.enums.PaymentType;
import org.springframework.stereotype.Component;

/**
 * Initiates Cash orders immediately ready for preparation and delivery collection.
 */
@Component
public class CashPaymentStrategy implements PaymentStrategy {

    @Override
    public boolean supports(PaymentType paymentType) {
        return paymentType == PaymentType.CASH;
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

