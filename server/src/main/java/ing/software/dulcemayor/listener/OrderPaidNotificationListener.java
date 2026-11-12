package ing.software.dulcemayor.listener;

import ing.software.dulcemayor.event.OrderPaidEvent;
import ing.software.dulcemayor.service.notification.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observer listener that reacts to OrderPaidEvent after transaction commit.
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
}

