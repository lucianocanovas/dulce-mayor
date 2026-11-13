package ing.software.dulcemayor.service.billing.factory;

import ing.software.dulcemayor.entity.billing.Invoice;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.enums.InvoiceStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Concrete Creator for Factura Electrónica Tipo B (AFIP Cód. 006).
 * Issued to customers under "Responsable Monotributo / Consumidor Final / Exento"
 * where VAT is included in the final price according to Argentine fiscal regulations.
 */
@Component
public class FacturaBCreator extends InvoiceCreator {

    public static final String INVOICE_TYPE_B = "B";
    public static final String AFIP_CODE_B = "006";
    public static final String RECEIVER_CONDITION_B = "Responsable Monotributo / Consumidor Final";

    @Override
    protected Invoice createInvoiceDocument(Order order, Long invoiceNumber) {
        double total = round2(order.getTotal());
        double netAmount = round2(total / (1.0 + VAT_RATE));
        double vatAmount = round2(total - netAmount);

        return Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .invoiceType(INVOICE_TYPE_B)
                .afipCode(AFIP_CODE_B)
                .taxConditionReceiver(RECEIVER_CONDITION_B)
                .invoiceDate(LocalDateTime.now())
                .netAmount(netAmount)
                .vatAmount(vatAmount)
                .total(total)
                .status(InvoiceStatus.PAID)
                .order(order)
                .invoiceDetails(new ArrayList<>())
                .build();
    }
}

