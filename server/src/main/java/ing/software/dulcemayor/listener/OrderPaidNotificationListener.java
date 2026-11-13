package ing.software.dulcemayor.listener;

import ing.software.dulcemayor.event.OrderDeliveredEvent;
import ing.software.dulcemayor.event.OrderPaidEvent;
import ing.software.dulcemayor.service.notification.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observer listener that reacts to OrderPaidEvent and OrderDeliveredEvent after transaction commit,
 * decoupling email notifications and next-stop logistics alerts from transactional domain services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaidNotificationListener {

    private final EmailNotificationService emailNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderPaid(OrderPaidEvent event) {
        log.debug("Observed OrderPaidEvent for order #{}", event.getOrderNumber());
        emailNotificationService.sendOrderConfirmationEmail(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderDelivered(OrderDeliveredEvent event) {
        log.info("Observed OrderDeliveredEvent for order #{} at '{}'. Next stop: '{}'",
                event.getOrderNumber(),
                event.getStoreName(),
                event.getNextStopStoreName() != null ? event.getNextStopStoreName() : "Ruta completada");
    }
}
