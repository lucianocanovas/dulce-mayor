package ing.software.dulcemayor.config;

import ing.software.dulcemayor.entity.billing.PaymentMethod;
import ing.software.dulcemayor.enums.PaymentType;
import ing.software.dulcemayor.repository.billing.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Ensures baseline PaymentMethod records exist for every supported PaymentType.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class PaymentDataInitializer implements CommandLineRunner {

    private static final Map<PaymentType, String> DEFAULT_NOTES = Map.of(
            PaymentType.CASH, "Pago en efectivo contra entrega o en sucursal",
            PaymentType.BANK_TRANSFER, "Transferencia bancaria por alias/CBU",
            PaymentType.DIGITAL_WALLET, "Billetera virtual / QR Mercado Pago",
            PaymentType.MERCADO_PAGO, "Checkout Pro / QR Mercado Pago"
    );

    private final PaymentMethodRepository paymentMethodRepository;

    @Override
    @Transactional
    public void run(String... args) {
        for (PaymentType type : PaymentType.values()) {
            paymentMethodRepository.findByPaymentTypeAndIsDeletedFalse(type)
                    .orElseGet(() -> paymentMethodRepository.save(PaymentMethod.builder()
                            .paymentType(type)
                            .note(DEFAULT_NOTES.getOrDefault(type, type.name()))
                            .build()));
        }
    }
}

