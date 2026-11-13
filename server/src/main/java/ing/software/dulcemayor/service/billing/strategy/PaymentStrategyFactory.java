package ing.software.dulcemayor.service.billing.strategy;

import ing.software.dulcemayor.dto.billing.OrderPaymentStatusDTO;
import ing.software.dulcemayor.dto.order.OrderResponseDTO;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.enums.PaymentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves the concrete PaymentStrategy for a requested PaymentType or string alias,
 * and centralizes polymorphic strategy delegation for display labels and DTO enrichment.
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

    public PaymentType resolvePaymentType(String raw) {
        if (raw == null || raw.isBlank()) {
            return PaymentType.CASH;
        }
        String normalized = raw.trim().toUpperCase();
        for (PaymentStrategy strategy : strategies) {
            if (strategy.matchesAlias(normalized)) {
                return strategy.resolveAliasType(normalized);
            }
        }
        return PaymentType.valueOf(normalized);
    }

    public String formatDisplayNameEs(PaymentType paymentType) {
        PaymentType effective = paymentType != null ? paymentType : PaymentType.CASH;
        return resolve(effective).getDisplayNameEs(effective);
    }

    public void enrichOrderResponse(Order order, OrderResponseDTO.OrderResponseDTOBuilder builder) {
        if (order != null) {
            resolve(order.getPaymentType()).enrichOrderResponse(order, builder);
        }
    }

    public void enrichPaymentStatus(Order order, OrderPaymentStatusDTO.OrderPaymentStatusDTOBuilder builder) {
        if (order != null) {
            resolve(order.getPaymentType()).enrichPaymentStatus(order, builder);
        }
    }
}
