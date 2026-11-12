package ing.software.dulcemayor.service.billing.strategy;

import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.enums.PaymentType;

/**
 * Strategy pattern abstraction for initiating payment flows per PaymentType.
 */
public interface PaymentStrategy {

    boolean supports(PaymentType paymentType);

    PaymentInitiationResult initiate(Order order, String returnBaseUrl);
}

