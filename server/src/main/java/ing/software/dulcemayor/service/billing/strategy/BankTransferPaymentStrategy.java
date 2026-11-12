package ing.software.dulcemayor.service.billing.strategy;

import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.enums.PaymentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Initiates Bank Transfer orders awaiting administrative confirmation of transfer receipt.
 */
@Component
public class BankTransferPaymentStrategy implements PaymentStrategy {

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
}

