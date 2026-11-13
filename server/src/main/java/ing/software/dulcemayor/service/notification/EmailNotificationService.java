package ing.software.dulcemayor.service.notification;

import ing.software.dulcemayor.event.OrderPaidEvent;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Sends asynchronous transactional confirmation emails when SMTP is enabled and configured.
 * Fails softly so payment accreditation never rolls back on email errors.
 */
@Service
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@dulcemayor.com.ar}")
    private String mailFrom;

    @Value("${app.mail.sender-name:Dulce Mayor Mendoza}")
    private String senderName;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public EmailNotificationService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean isMailSenderAvailable() {
        return mailEnabled
                && mailSender != null
                && mailUsername != null && !mailUsername.isBlank()
                && mailPassword != null && !mailPassword.isBlank();
    }

    @Async
    public void sendOrderConfirmationEmail(OrderPaidEvent event) {
        if (event == null || event.getCustomerEmail() == null || event.getCustomerEmail().isBlank()) {
            log.debug("Skipping order confirmation email: no recipient address for order {}",
                    event != null ? event.getOrderId() : "unknown");
            return;
        }

        if (!isMailSenderAvailable()) {
            log.info("[Mail Disabled/Not Configured] Confirmation preview -> to={}, order=#{}, invoice=#{}, total={}",
                    event.getCustomerEmail(), event.getOrderNumber(), event.getInvoiceNumber(), event.getTotal());
            return;
        }

        String subject = "Dulce Mayor — Pago acreditado Pedido #" + event.getOrderNumber();
        sendHtmlEmail(event.getCustomerEmail().trim(), subject, buildHtmlBody(event));
    }

    @Async
    public void sendEmail(String to, String subject, String text) {
        if (!isMailSenderAvailable()) {
            log.info("[Mail Simulator] Plain email -> to={}, subject={}", to, subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom != null && !mailFrom.isBlank() ? mailFrom : mailUsername);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Plain email sent to {} (subject={})", to, subject);
        } catch (Exception ex) {
            log.warn("Could not send plain email to {}: {}", to, ex.getMessage());
        }
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (!isMailSenderAvailable()) {
            log.info("[Mail Simulator] HTML email -> to={}, subject={}", to, subject);
            return;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            String fromAddress = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom : mailUsername;
            helper.setFrom(fromAddress, senderName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("HTML confirmation email sent to {} (subject={})", to, subject);
        } catch (Exception ex) {
            log.warn("Could not send HTML email to {}: {}", to, ex.getMessage());
        }
    }

    private String buildHtmlBody(OrderPaidEvent event) {
        String recipient = HtmlUtils.htmlEscape(
                event.getCustomerName() != null && !event.getCustomerName().isBlank()
                        ? event.getCustomerName()
                        : "Cliente");
        String store = HtmlUtils.htmlEscape(event.getStoreName() != null ? event.getStoreName() : "-");
        String address = HtmlUtils.htmlEscape(
                event.getDeliveryAddress() != null ? event.getDeliveryAddress() : "A coordinar");
        String method = HtmlUtils.htmlEscape(
                event.getPaymentType() != null ? event.getPaymentType().name() : "-");
        String ref = HtmlUtils.htmlEscape(
                event.getPaymentReference() != null ? event.getPaymentReference() : "-");
        long orderNum = event.getOrderNumber() != null ? event.getOrderNumber() : 0L;
        long invoiceNum = event.getInvoiceNumber() != null ? event.getInvoiceNumber() : 0L;

        return String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #FAF6F0; padding: 24px; border-radius: 12px; color: #2B1810;">
                  <h2 style="color: #F58721; margin-top: 0;">Dulce Mayor — Pago Acreditado</h2>
                  <p>Hola <strong>%s</strong>,</p>
                  <p>Confirmamos la acreditación de tu pago. Tu pedido ya pasó a preparación y distribución.</p>
                  <table style="width: 100%%; border-collapse: collapse; background: #FFFFFF; border-radius: 8px; overflow: hidden; margin: 16px 0;">
                    <tr><td style="padding: 10px 14px; border-bottom: 1px solid #F0E6D8;"><strong>Pedido N°</strong></td><td style="padding: 10px 14px; border-bottom: 1px solid #F0E6D8;">#%d</td></tr>
                    <tr><td style="padding: 10px 14px; border-bottom: 1px solid #F0E6D8;"><strong>Factura N°</strong></td><td style="padding: 10px 14px; border-bottom: 1px solid #F0E6D8;">#%d</td></tr>
                    <tr><td style="padding: 10px 14px; border-bottom: 1px solid #F0E6D8;"><strong>Comercio / Destino</strong></td><td style="padding: 10px 14px; border-bottom: 1px solid #F0E6D8;">%s</td></tr>
                    <tr><td style="padding: 10px 14px; border-bottom: 1px solid #F0E6D8;"><strong>Dirección de entrega</strong></td><td style="padding: 10px 14px; border-bottom: 1px solid #F0E6D8;">%s</td></tr>
                    <tr><td style="padding: 10px 14px; border-bottom: 1px solid #F0E6D8;"><strong>Medio de pago</strong></td><td style="padding: 10px 14px; border-bottom: 1px solid #F0E6D8;">%s (Ref: %s)</td></tr>
                    <tr><td style="padding: 10px 14px;"><strong>Total abonado</strong></td><td style="padding: 10px 14px; font-weight: bold; color: #F58721;">$%.2f</td></tr>
                  </table>
                  <p style="font-size: 13px; color: #6E5649;">Saludos,<br/>Equipo Dulce Mayor Mendoza</p>
                </div>
                """,
                recipient, orderNum, invoiceNum, store, address, method, ref, event.getTotal());
    }
}
