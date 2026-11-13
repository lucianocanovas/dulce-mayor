package ing.software.dulcemayor.service.billing.factory;

import ing.software.dulcemayor.entity.billing.Invoice;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.enums.InvoiceStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Concrete Creator for Factura Electrónica Tipo A (AFIP Cód. 001).
 * Issued to wholesale customers registered as "IVA Responsable Inscripto" (valid 11-digit CUIT),
 * discriminating Net Taxable Amount (Importe Neto Gravado) and 21% VAT (IVA 21%).
 */
@Component
public class FacturaACreator extends InvoiceCreator {

    public static final String INVOICE_TYPE_A = "A";
    public static final String AFIP_CODE_A = "001";
    public static final String RECEIVER_CONDITION_A = "IVA Responsable Inscripto";

    @Override
    protected Invoice createInvoiceDocument(Order order, Long invoiceNumber) {
        double total = round2(order.getTotal());
        double netAmount = round2(total / (1.0 + VAT_RATE));
        double vatAmount = round2(total - netAmount);

        return Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .invoiceType(INVOICE_TYPE_A)
                .afipCode(AFIP_CODE_A)
                .taxConditionReceiver(RECEIVER_CONDITION_A)
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

