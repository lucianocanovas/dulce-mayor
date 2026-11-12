package ing.software.dulcemayor.service.billing.strategy;

import ing.software.dulcemayor.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

/**
 * Outcome returned by a PaymentStrategy when initiating an order payment flow.
 */
@Getter
@Builder
public class PaymentInitiationResult {
    private final OrderStatus initialOrderStatus;
    private final boolean readyForDispatch;
    private final String preferenceId;
    private final String initPoint;
    private final String qrPayload;
    private final String qrImageBase64;
    private final String bankAlias;
    private final String bankCbu;
    private final String bankHolder;
    private final String bankName;
    private final String message;
}

