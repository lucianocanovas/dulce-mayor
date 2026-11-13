package ing.software.dulcemayor.service.billing.factory;

import ing.software.dulcemayor.entity.billing.Invoice;
import ing.software.dulcemayor.entity.billing.InvoiceDetail;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.entity.order.OrderDetail;
import ing.software.dulcemayor.enums.InvoiceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Abstract Creator of the Factory Method creational pattern for electronic invoices.
 * Subclasses ({@link FacturaACreator}, {@link FacturaBCreator}) implement the factory method
 * {@link #createInvoiceDocument(Order, Long)} to instantiate and configure the specific fiscal document.
 */
public abstract class InvoiceCreator {

    protected static final double VAT_RATE = 0.21;

    /**
     * Factory Method to be implemented by concrete creators (Factura A, Factura B, etc.).
     *
     * @param order         the billed order
     * @param invoiceNumber sequential invoice number
     * @return configured {@link Invoice} instance
     */
    protected abstract Invoice createInvoiceDocument(Order order, Long invoiceNumber);

    /**
     * Business operation that relies on the Factory Method {@link #createInvoiceDocument(Order, Long)}
     * to construct the complete invoice with its fiscal metadata, CAE authorization, and line items.
     */
    public final Invoice issueInvoice(Order order, Long invoiceNumber) {
        if (order == null) {
            throw new IllegalArgumentException("No se puede emitir una factura sin un pedido asociado.");
        }
        if (invoiceNumber == null || invoiceNumber <= 0) {
            throw new IllegalArgumentException("El número de comprobante fiscal debe ser mayor a cero.");
        }

        Invoice invoice = createInvoiceDocument(order, invoiceNumber);

        LocalDateTime emissionDateTime = invoice.getInvoiceDate() != null
                ? invoice.getInvoiceDate()
                : LocalDateTime.now();
        LocalDate emissionDate = emissionDateTime.toLocalDate();

        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setInvoiceDate(emissionDateTime);
        invoice.setOrder(order);
        if (invoice.getStatus() == null) {
            invoice.setStatus(InvoiceStatus.PAID);
        }
        if (invoice.getCaeNumber() == null || invoice.getCaeNumber().isBlank()) {
            invoice.setCaeNumber(generateDeterministicCae(invoiceNumber, emissionDate));
        }
        if (invoice.getCaeExpirationDate() == null) {
            invoice.setCaeExpirationDate(emissionDate.plusDays(10));
        }
        if (invoice.getInvoiceDetails() == null) {
            invoice.setInvoiceDetails(new ArrayList<>());
        }

        if (order.getOrderDetails() != null && invoice.getInvoiceDetails().isEmpty()) {
            for (OrderDetail orderDetail : order.getOrderDetails()) {
                if (orderDetail.isDeleted()) {
                    continue;
                }
                InvoiceDetail detail = createInvoiceDetail(invoice, orderDetail);
                invoice.getInvoiceDetails().add(detail);
            }
        }

        return invoice;
    }

    /**
     * Hook method that creates each {@link InvoiceDetail} line associated with the created {@link Invoice}.
     */
    protected InvoiceDetail createInvoiceDetail(Invoice invoice, OrderDetail orderDetail) {
        String code = orderDetail.getProduct() != null ? orderDetail.getProduct().getCode() : "ITEM";
        String name = orderDetail.getProduct() != null ? orderDetail.getProduct().getName() : "Producto Mayorista";

        return InvoiceDetail.builder()
                .quantity(orderDetail.getQuantity())
                .unitPrice(orderDetail.getUnitPrice())
                .subtotal(orderDetail.getSubtotal())
                .product(orderDetail.getProduct())
                .productCode(code)
                .productName(name)
                .invoice(invoice)
                .build();
    }

    protected double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    protected String generateDeterministicCae(Long invoiceNumber, LocalDate date) {
        long num = invoiceNumber != null ? invoiceNumber : 10001L;
        int yearShort = date.getYear() % 100;
        int dayOfYear = date.getDayOfYear();
        long tail = (num * 7919L + dayOfYear * 131L) % 100000000L;
        return String.format("74%02d%02d%08d", yearShort, date.getMonthValue(), tail);
    }
}

