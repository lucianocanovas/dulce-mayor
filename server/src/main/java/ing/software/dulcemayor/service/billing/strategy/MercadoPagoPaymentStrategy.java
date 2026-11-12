package ing.software.dulcemayor.service.billing.strategy;

import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.enums.PaymentType;
import ing.software.dulcemayor.service.billing.MercadoPagoService;
import ing.software.dulcemayor.service.billing.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Initiates Mercado Pago Checkout Pro / QR payment flows via the direct REST API.
 */
@Component
@RequiredArgsConstructor
public class MercadoPagoPaymentStrategy implements PaymentStrategy {

    private final MercadoPagoService mercadoPagoService;
    private final QrCodeService qrCodeService;

    @Override
    public boolean supports(PaymentType paymentType) {
        return paymentType == PaymentType.MERCADO_PAGO || paymentType == PaymentType.DIGITAL_WALLET;
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

