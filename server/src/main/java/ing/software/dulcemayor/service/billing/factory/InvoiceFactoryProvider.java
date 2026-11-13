package ing.software.dulcemayor.service.billing.factory;

import ing.software.dulcemayor.entity.actor.Retailer;
import ing.software.dulcemayor.entity.billing.Invoice;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.entity.order.PointOfSale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the appropriate {@link InvoiceCreator} (Factory Method Concrete Creator)
 * according to the fiscal condition of the customer/order and delegates invoice creation.
 */
@Component
@RequiredArgsConstructor
public class InvoiceFactoryProvider {

    private final FacturaACreator facturaACreator;
    private final FacturaBCreator facturaBCreator;

    /**
     * Resolves the concrete {@link InvoiceCreator} for the given {@link Order}.
     * Orders with a valid 11-digit CUIT receive a {@link FacturaACreator} (Factura A - COD. 001);
     * all others receive a {@link FacturaBCreator} (Factura B - COD. 006).
     */
    public InvoiceCreator resolveCreator(Order order) {
        String document = extractCustomerTaxDocument(order);
        if (isCuitResponsableInscripto(document)) {
            return facturaACreator;
        }
        return facturaBCreator;
    }

    /**
     * Creates and configures the {@link Invoice} and its {@link ing.software.dulcemayor.entity.billing.InvoiceDetail}
     * items for the given {@link Order} using the resolved Factory Method creator.
     */
    public Invoice createInvoiceForOrder(Order order, Long invoiceNumber) {
        InvoiceCreator creator = resolveCreator(order);
        return creator.issueInvoice(order, invoiceNumber);
    }

    public String extractCustomerTaxDocument(Order order) {
        if (order == null) {
            return null;
        }
        PointOfSale pos = order.getPointOfSale();
        if (pos != null) {
            if (pos.getTaxIdOrDni() != null && !pos.getTaxIdOrDni().isBlank()) {
                return pos.getTaxIdOrDni().trim();
            }
            Retailer retailer = pos.getRetailer();
            if (retailer != null && retailer.getDocumentNumber() != null && !retailer.getDocumentNumber().isBlank()) {
                return retailer.getDocumentNumber().trim();
            }
        }
        return null;
    }

    public boolean isCuitResponsableInscripto(String doc) {
        if (doc == null || doc.isBlank()) {
            return false;
        }
        String digits = doc.replaceAll("\\D", "");
        return digits.length() == 11
                && (digits.startsWith("20")
                || digits.startsWith("23")
                || digits.startsWith("24")
                || digits.startsWith("27")
                || digits.startsWith("30")
                || digits.startsWith("33")
                || digits.startsWith("34"));
    }
}

