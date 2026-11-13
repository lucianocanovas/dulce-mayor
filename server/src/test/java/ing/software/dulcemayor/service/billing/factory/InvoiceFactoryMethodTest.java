package ing.software.dulcemayor.service.billing.factory;

import ing.software.dulcemayor.entity.actor.Retailer;
import ing.software.dulcemayor.entity.billing.Invoice;
import ing.software.dulcemayor.entity.catalog.Product;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.entity.order.OrderDetail;
import ing.software.dulcemayor.entity.order.PointOfSale;
import ing.software.dulcemayor.enums.InvoiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceFactoryMethodTest {

    private FacturaACreator facturaACreator;
    private FacturaBCreator facturaBCreator;
    private InvoiceFactoryProvider factoryProvider;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        facturaACreator = new FacturaACreator();
        facturaBCreator = new FacturaBCreator();
        factoryProvider = new InvoiceFactoryProvider(facturaACreator, facturaBCreator);
        sampleProduct = Product.builder()
                .code("ARC-BON-01")
                .name("Bon o Bon Leche")
                .packaging("Display x 24 u.")
                .unitsPerPack(24)
                .build();
    }

    @Test
    @DisplayName("Factory Method: Retailer with 11-digit CUIT resolves FacturaACreator and emits Factura A (COD. 001)")
    void createInvoiceForCuitOrder_usesFacturaACreator() {
        Retailer retailer = Retailer.builder()
                .firstName("Roberto")
                .lastName("Gómez")
                .documentNumber("20-28491823-4")
                .build();
        PointOfSale pos = PointOfSale.builder()
                .storeName("Kiosco El Trébol")
                .taxIdOrDni("20-28491823-4")
                .retailer(retailer)
                .build();
        Order order = Order.builder()
                .orderNumber(1001L)
                .total(12100.0)
                .pointOfSale(pos)
                .orderDetails(List.of(OrderDetail.builder()
                        .product(sampleProduct)
                        .quantity(1)
                        .unitPrice(12100.0)
                        .subtotal(12100.0)
                        .build()))
                .build();

        InvoiceCreator resolved = factoryProvider.resolveCreator(order);
        assertThat(resolved).isInstanceOf(FacturaACreator.class);

        Invoice invoice = factoryProvider.createInvoiceForOrder(order, 10001L);
        assertThat(invoice.getInvoiceNumber()).isEqualTo(10001L);
        assertThat(invoice.getInvoiceType()).isEqualTo("A");
        assertThat(invoice.getAfipCode()).isEqualTo("001");
        assertThat(invoice.getTaxConditionReceiver()).isEqualTo("IVA Responsable Inscripto");
        assertThat(invoice.getNetAmount()).isEqualTo(10000.0);
        assertThat(invoice.getVatAmount()).isEqualTo(2100.0);
        assertThat(invoice.getTotal()).isEqualTo(12100.0);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getCaeNumber()).hasSize(14);
        assertThat(invoice.getCaeExpirationDate()).isNotNull();
        assertThat(invoice.getInvoiceDetails()).hasSize(1);
        assertThat(invoice.getInvoiceDetails().get(0).getProductCode()).isEqualTo("ARC-BON-01");
    }

    @Test
    @DisplayName("Factory Method: Customer with DNI or without CUIT resolves FacturaBCreator and emits Factura B (COD. 006)")
    void createInvoiceForDniOrder_usesFacturaBCreator() {
        PointOfSale pos = PointOfSale.builder()
                .storeName("Almacén Barrio")
                .taxIdOrDni("34891220")
                .build();
        Order order = Order.builder()
                .orderNumber(1002L)
                .total(24200.0)
                .pointOfSale(pos)
                .orderDetails(List.of(OrderDetail.builder()
                        .product(sampleProduct)
                        .quantity(2)
                        .unitPrice(12100.0)
                        .subtotal(24200.0)
                        .build()))
                .build();

        InvoiceCreator resolved = factoryProvider.resolveCreator(order);
        assertThat(resolved).isInstanceOf(FacturaBCreator.class);

        Invoice invoice = factoryProvider.createInvoiceForOrder(order, 10002L);
        assertThat(invoice.getInvoiceNumber()).isEqualTo(10002L);
        assertThat(invoice.getInvoiceType()).isEqualTo("B");
        assertThat(invoice.getAfipCode()).isEqualTo("006");
        assertThat(invoice.getTaxConditionReceiver()).isEqualTo("Responsable Monotributo / Consumidor Final");
        assertThat(invoice.getNetAmount()).isEqualTo(20000.0);
        assertThat(invoice.getVatAmount()).isEqualTo(4200.0);
        assertThat(invoice.getTotal()).isEqualTo(24200.0);
        assertThat(invoice.getInvoiceDetails()).hasSize(1);
    }
}

