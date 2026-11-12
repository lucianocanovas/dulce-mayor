package ing.software.dulcemayor.service.billing;

import ing.software.dulcemayor.dto.billing.OrderPaymentStatusDTO;
import ing.software.dulcemayor.dto.order.CreateOrderItemDTO;
import ing.software.dulcemayor.dto.order.CreateOrderRequestDTO;
import ing.software.dulcemayor.dto.order.OrderResponseDTO;
import ing.software.dulcemayor.entity.actor.DeliveryAgent;
import ing.software.dulcemayor.entity.actor.Retailer;
import ing.software.dulcemayor.entity.billing.Invoice;
import ing.software.dulcemayor.entity.billing.Payment;
import ing.software.dulcemayor.entity.catalog.Category;
import ing.software.dulcemayor.entity.catalog.PriceValidity;
import ing.software.dulcemayor.entity.catalog.Product;
import ing.software.dulcemayor.entity.catalog.Stock;
import ing.software.dulcemayor.entity.catalog.SubCategory;
import ing.software.dulcemayor.entity.location.Address;
import ing.software.dulcemayor.entity.location.City;
import ing.software.dulcemayor.entity.location.Country;
import ing.software.dulcemayor.entity.location.Department;
import ing.software.dulcemayor.entity.location.State;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.entity.order.PointOfSale;
import ing.software.dulcemayor.enums.DocumentType;
import ing.software.dulcemayor.enums.InvoiceStatus;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.enums.PaymentType;
import ing.software.dulcemayor.repository.actor.DeliveryAgentRepository;
import ing.software.dulcemayor.repository.actor.RetailerRepository;
import ing.software.dulcemayor.repository.billing.InvoiceRepository;
import ing.software.dulcemayor.repository.billing.PaymentRepository;
import ing.software.dulcemayor.repository.catalog.CategoryRepository;
import ing.software.dulcemayor.repository.catalog.PriceValidityRepository;
import ing.software.dulcemayor.repository.catalog.ProductRepository;
import ing.software.dulcemayor.repository.catalog.StockRepository;
import ing.software.dulcemayor.repository.catalog.SubCategoryRepository;
import ing.software.dulcemayor.repository.location.CityRepository;
import ing.software.dulcemayor.repository.location.CountryRepository;
import ing.software.dulcemayor.repository.location.DepartmentRepository;
import ing.software.dulcemayor.repository.location.StateRepository;
import ing.software.dulcemayor.repository.order.OrderRepository;
import ing.software.dulcemayor.repository.order.PointOfSaleRepository;
import ing.software.dulcemayor.service.notification.EmailNotificationService;
import ing.software.dulcemayor.service.order.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:paymenttestdb;DB_CLOSE_DELAY=-1")
class PaymentAccreditationServiceTest {

    @Autowired private PaymentAccreditationService accreditationService;
    @Autowired private OrderService orderService;
    @Autowired private CountryRepository countryRepository;
    @Autowired private StateRepository stateRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private RetailerRepository retailerRepository;
    @Autowired private PointOfSaleRepository pointOfSaleRepository;
    @Autowired private DeliveryAgentRepository deliveryAgentRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private SubCategoryRepository subCategoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private PriceValidityRepository priceValidityRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private PaymentRepository paymentRepository;

    @MockitoSpyBean
    private EmailNotificationService emailNotificationService;

    private Product seededProduct;
    private PointOfSale seededPos;

    @BeforeEach
    void setUpFixtures() {
        Country country = countryRepository.save(Country.builder().name("Argentina").build());
        State state = stateRepository.save(State.builder().name("Mendoza").country(country).build());
        Department dept = departmentRepository.save(Department.builder().name("Capital").state(state).build());
        City city = cityRepository.save(City.builder().name("Mendoza Capital").postalCode("5500").department(dept).build());

        if (deliveryAgentRepository.findByIsDeletedFalse().isEmpty()) {
            deliveryAgentRepository.save(DeliveryAgent.builder()
                    .firstName("Carlos")
                    .lastName("Reparto")
                    .documentType(DocumentType.DNI)
                    .documentNumber("30-11111111-1")
                    .build());
        }

        Retailer retailer = retailerRepository.save(Retailer.builder()
                .firstName("Roberto")
                .lastName("Gómez")
                .documentType(DocumentType.DNI)
                .documentNumber("20-28491823-4")
                .build());
        Address addr = Address.builder()
                .street("Av. San Martín 1420")
                .neighborhood("Centro")
                .city(city)
                .latitude(-32.8895)
                .longitude(-68.8458)
                .build();
        seededPos = pointOfSaleRepository.save(PointOfSale.builder()
                .storeName("Kiosco Test Accred")
                .retailer(retailer)
                .address(addr)
                .build());

        Category cat = categoryRepository.save(Category.builder().name("Chocolates").build());
        SubCategory sub = subCategoryRepository.save(SubCategory.builder().name("Bombones").category(cat).build());
        String uniqueCode = "ACC-" + System.nanoTime();
        seededProduct = productRepository.save(Product.builder()
                .code(uniqueCode)
                .name("Bon o Bon Test")
                .packaging("Display x 24 u.")
                .unitsPerPack(24)
                .subCategory(sub)
                .build());
        priceValidityRepository.save(PriceValidity.builder()
                .product(seededProduct)
                .price(14200.0)
                .startDate(LocalDate.now().minusDays(1))
                .build());
        stockRepository.save(Stock.builder()
                .product(seededProduct)
                .currentQuantity(50)
                .build());
    }

    @Test
    @DisplayName("Idempotent accreditation: duplicate triggers deduct stock once, emit one invoice/payment, notify once, and enable dispatch")
    void accreditPaidOrder_isStrictlyIdempotent() {
        int qtyToOrder = 2;
        int initialQty = stockRepository.findByProductIdAndIsDeletedFalse(seededProduct.getId())
                .orElseThrow().getCurrentQuantity();

        OrderResponseDTO created = orderService.createOrder(CreateOrderRequestDTO.builder()
                .pointOfSaleId(seededPos.getId())
                .paymentType("BANK_TRANSFER")
                .items(List.of(CreateOrderItemDTO.builder()
                        .productId(seededProduct.getId())
                        .quantity(qtyToOrder)
                        .build()))
                .build());

        assertThat(created.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(created.isStockDeducted()).isFalse();
        assertThat(created.isReadyForDispatch()).isFalse();

        int qtyAfterPending = stockRepository.findByProductIdAndIsDeletedFalse(seededProduct.getId())
                .orElseThrow().getCurrentQuantity();
        assertThat(qtyAfterPending).isEqualTo(initialQty);

        // Accredit twice (simulating webhook + polling race)
        OrderPaymentStatusDTO first = accreditationService.accreditPaidOrder(
                created.getId(), PaymentType.MERCADO_PAGO, "MP-777001", "approved", "MP-777001", "First trigger");
        OrderPaymentStatusDTO second = accreditationService.accreditPaidOrder(
                created.getId(), PaymentType.MERCADO_PAGO, "MP-777001", "approved", "MP-777001", "Duplicate trigger");

        assertThat(first.isPaid()).isTrue();
        assertThat(first.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(first.isReadyForDispatch()).isTrue();
        assertThat(first.getInvoiceNumber()).isNotNull();

        assertThat(second.isPaid()).isTrue();
        assertThat(second.getInvoiceId()).isEqualTo(first.getInvoiceId());
        assertThat(second.getInvoiceNumber()).isEqualTo(first.getInvoiceNumber());

        int qtyAfterAccreditation = stockRepository.findByProductIdAndIsDeletedFalse(seededProduct.getId())
                .orElseThrow().getCurrentQuantity();
        assertThat(qtyAfterAccreditation).isEqualTo(initialQty - qtyToOrder);

        Order persisted = orderRepository.findByIdAndIsDeletedFalse(created.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(persisted.isStockDeducted()).isTrue();
        assertThat(persisted.isReadyForDispatch()).isTrue();
        assertThat(persisted.getDeliveryRoute()).isNotNull();

        Invoice invoice = invoiceRepository.findByOrderIdAndIsDeletedFalse(created.getId()).orElseThrow();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);

        var invoiceDto = accreditationService.getInvoiceForOrder(created.getId());
        assertThat(invoiceDto.getInvoiceNumber()).isEqualTo(invoice.getInvoiceNumber());
        assertThat(invoiceDto.getDetails()).hasSize(1);
        assertThat(invoiceDto.getDetails().get(0).getQuantity()).isEqualTo(qtyToOrder);

        List<Payment> payments = paymentRepository.findByInvoiceIdAndIsDeletedFalse(invoice.getId());
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getReference()).isEqualTo("MP-777001");
        assertThat(invoiceDto.getPaymentType()).isEqualTo(PaymentType.MERCADO_PAGO.name());
        assertThat(invoiceDto.getPaymentReference()).isEqualTo("MP-777001");

        verify(emailNotificationService, times(1)).sendOrderConfirmationEmail(any());
    }
}
