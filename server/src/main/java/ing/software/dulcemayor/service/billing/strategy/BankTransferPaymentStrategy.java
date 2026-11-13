package ing.software.dulcemayor.service.billing.strategy;

import ing.software.dulcemayor.dto.billing.OrderPaymentStatusDTO;
import ing.software.dulcemayor.dto.order.OrderResponseDTO;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.enums.PaymentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Initiates Bank Transfer orders awaiting administrative confirmation of transfer receipt
 * and encapsulates bank account metadata for DTO enrichment.
 */
@Component
public class BankTransferPaymentStrategy implements PaymentStrategy {

    private static final Set<String> ALIASES = Set.of("TRANSFER", "TRANSFERENCIA", "BANK_TRANSFER");

    @Value("${payment.bank.alias:DULCE.MAYOR.MZA}")
    private String bankAlias;

    @Value("${payment.bank.cbu:0000003100084920193842}")
    private String bankCbu;

    @Value("${payment.bank.holder:Dulce Mayor S.A.}")
    private String bankHolder;

    @Value("${payment.bank.name:Banco de la Nación Argentina}")
    private String bankName;

    @Override
    public boolean supports(PaymentType paymentType) {
        return paymentType == PaymentType.BANK_TRANSFER;
    }

    @Override
    public boolean matchesAlias(String normalizedAlias) {
        return normalizedAlias != null && ALIASES.contains(normalizedAlias);
    }

    @Override
    public PaymentType resolveAliasType(String normalizedAlias) {
        return PaymentType.BANK_TRANSFER;
    }

    @Override
    public OrderStatus getInitialOrderStatus() {
        return OrderStatus.PENDING_PAYMENT;
    }

    @Override
    public String getDisplayNameEs(PaymentType specificType) {
        return "Transferencia Bancaria";
    }

    @Override
    public PaymentInitiationResult initiate(Order order, String returnBaseUrl) {
        return PaymentInitiationResult.builder()
                .initialOrderStatus(OrderStatus.PENDING_PAYMENT)
                .readyForDispatch(false)
                .bankAlias(bankAlias)
                .bankCbu(bankCbu)
                .bankHolder(bankHolder)
                .bankName(bankName)
                .message("Pedido registrado. Realizá la transferencia al alias " + bankAlias
                        + " y aguardá confirmación administrativa.")
                .build();
    }

    @Override
    public void enrichOrderResponse(Order order, OrderResponseDTO.OrderResponseDTOBuilder builder) {
        builder.bankAlias(bankAlias)
                .bankCbu(bankCbu)
                .bankHolder(bankHolder);
    }

    @Override
    public void enrichPaymentStatus(Order order, OrderPaymentStatusDTO.OrderPaymentStatusDTOBuilder builder) {
        builder.bankAlias(bankAlias)
                .bankCbu(bankCbu)
                .bankHolder(bankHolder);
    }
}
