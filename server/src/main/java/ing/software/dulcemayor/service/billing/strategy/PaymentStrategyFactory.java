package ing.software.dulcemayor.service.billing.strategy;

import ing.software.dulcemayor.enums.PaymentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves the concrete PaymentStrategy for a requested PaymentType.
 */
@Component
@RequiredArgsConstructor
public class PaymentStrategyFactory {

    private final List<PaymentStrategy> strategies;

    public PaymentStrategy resolve(PaymentType paymentType) {
        PaymentType effective = paymentType != null ? paymentType : PaymentType.CASH;
        return strategies.stream()
                .filter(s -> s.supports(effective))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported payment type: " + effective));
    }
}

