package ing.software.dulcemayor.service.notification;

import ing.software.dulcemayor.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends transactional customer confirmation emails when SMTP is enabled and configured.
 * Fails softly so payment accreditation never rolls back on email errors.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@dulcemayor.com.ar}")
    private String mailFrom;

    public void sendOrderConfirmationEmail(OrderPaidEvent event) {
        if (event == null || event.getCustomerEmail() == null || event.getCustomerEmail().isBlank()) {
            log.debug("Skipping order confirmation email: no recipient address for order {}",
                    event != null ? event.getOrderId() : "unknown");
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!mailEnabled || mailSender == null) {
            log.info("[Mail Disabled/Not Configured] Confirmation preview -> to={}, order=#{}, invoice=#{}, total={}",
                    event.getCustomerEmail(), event.getOrderNumber(), event.getInvoiceNumber(), event.getTotal());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(event.getCustomerEmail().trim());
            message.setSubject("Dulce Mayor — Pago acreditado Pedido #" + event.getOrderNumber());
            message.setText(buildEmailBody(event));
            mailSender.send(message);
            log.info("Order confirmation email sent to {} for order #{}", event.getCustomerEmail(), event.getOrderNumber());
        } catch (Exception ex) {
            log.warn("Could not send confirmation email for order #{} to {}: {}",
                    event.getOrderNumber(), event.getCustomerEmail(), ex.getMessage());
        }
    }

    private String buildEmailBody(OrderPaidEvent event) {
        String recipient = event.getCustomerName() != null && !event.getCustomerName().isBlank()
                ? event.getCustomerName()
                : "Cliente";
        return String.format(
                "Hola %s,\n\n"
                        + "Confirmamos la acreditación de tu pago en Dulce Mayor.\n\n"
                        + "• Pedido N°: #%d\n"
                        + "• Comprobante / Factura N°: #%d\n"
                        + "• Comercio / Destino: %s\n"
                        + "• Dirección de entrega: %s\n"
                        + "• Medio de pago: %s (Ref: %s)\n"
                        + "• Total abonado: $%.2f\n\n"
                        + "Tu pedido ya pasó a preparación y distribución.\n\n"
                        + "Saludos,\nEquipo Dulce Mayor",
                recipient,
                event.getOrderNumber() != null ? event.getOrderNumber() : 0L,
                event.getInvoiceNumber() != null ? event.getInvoiceNumber() : 0L,
                event.getStoreName() != null ? event.getStoreName() : "-",
                event.getDeliveryAddress() != null ? event.getDeliveryAddress() : "A coordinar",
                event.getPaymentType() != null ? event.getPaymentType().name() : "-",
                event.getPaymentReference() != null ? event.getPaymentReference() : "-",
                event.getTotal()
        );
    }
}

