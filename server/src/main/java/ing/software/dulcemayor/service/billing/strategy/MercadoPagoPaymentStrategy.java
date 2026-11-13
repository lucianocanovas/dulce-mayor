package ing.software.dulcemayor.service.billing.strategy;

import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.enums.PaymentType;
import ing.software.dulcemayor.service.billing.MercadoPagoService;
import ing.software.dulcemayor.service.billing.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Initiates Mercado Pago Checkout Pro / QR payment flows via the direct REST API.
 */
@Component
@RequiredArgsConstructor
public class MercadoPagoPaymentStrategy implements PaymentStrategy {

    private static final Set<String> MP_ALIASES = Set.of("MP", "MERCADOPAGO", "MERCADO_PAGO");
    private static final Set<String> WALLET_ALIASES = Set.of("QR", "BILLETERA_VIRTUAL", "DIGITAL_WALLET");

    private final MercadoPagoService mercadoPagoService;
    private final QrCodeService qrCodeService;

    @Override
    public boolean supports(PaymentType paymentType) {
        return paymentType == PaymentType.MERCADO_PAGO || paymentType == PaymentType.DIGITAL_WALLET;
    }

    @Override
    public boolean matchesAlias(String normalizedAlias) {
        return normalizedAlias != null && (MP_ALIASES.contains(normalizedAlias) || WALLET_ALIASES.contains(normalizedAlias));
    }

    @Override
    public PaymentType resolveAliasType(String normalizedAlias) {
        if (normalizedAlias != null && WALLET_ALIASES.contains(normalizedAlias)) {
            return PaymentType.DIGITAL_WALLET;
        }
        return PaymentType.MERCADO_PAGO;
    }

    @Override
    public OrderStatus getInitialOrderStatus() {
        return OrderStatus.PENDING_PAYMENT;
    }

    @Override
    public String getDisplayNameEs(PaymentType specificType) {
        return specificType == PaymentType.DIGITAL_WALLET
                ? "Billetera Virtual / QR"
                : "Mercado Pago";
    }

    @Override
    public PaymentInitiationResult initiate(Order order, String returnBaseUrl) {
        MercadoPagoService.PreferenceResult preference = mercadoPagoService.createPreference(order, returnBaseUrl);
        String initPoint = preference.initPoint();
        String qrBase64 = qrCodeService.generateQrBase64(initPoint, 320, 320);

        return PaymentInitiationResult.builder()
                .initialOrderStatus(OrderStatus.PENDING_PAYMENT)
                .readyForDispatch(false)
                .preferenceId(preference.preferenceId())
                .initPoint(initPoint)
                .qrPayload(initPoint)
                .qrImageBase64(qrBase64)
                .message("Preferencia de Mercado Pago generada. Esperando confirmación del pago.")
                .build();
    }
}
