package ing.software.dulcemayor.entity;

import ing.software.dulcemayor.entity.actor.DeliveryAgent;
import ing.software.dulcemayor.entity.actor.Retailer;
import ing.software.dulcemayor.entity.actor.SalesRepresentative;
import ing.software.dulcemayor.entity.actor.User;
import ing.software.dulcemayor.entity.billing.Invoice;
import ing.software.dulcemayor.entity.billing.InvoiceDetail;
import ing.software.dulcemayor.entity.billing.Payment;
import ing.software.dulcemayor.entity.billing.PaymentMethod;
import ing.software.dulcemayor.entity.catalog.Category;
import ing.software.dulcemayor.entity.catalog.PriceValidity;
import ing.software.dulcemayor.entity.catalog.Product;
import ing.software.dulcemayor.entity.catalog.Promotion;
import ing.software.dulcemayor.entity.catalog.Stock;
import ing.software.dulcemayor.entity.catalog.SubCategory;
import ing.software.dulcemayor.entity.location.Address;
import ing.software.dulcemayor.entity.location.City;
import ing.software.dulcemayor.entity.location.Country;
import ing.software.dulcemayor.entity.location.Department;
import ing.software.dulcemayor.entity.location.State;
import ing.software.dulcemayor.entity.logistics.DeliveryRoute;
import ing.software.dulcemayor.entity.logistics.SalesRoute;
import ing.software.dulcemayor.entity.media.Image;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.entity.order.OrderDetail;
import ing.software.dulcemayor.entity.order.OrderStatusHistory;
import ing.software.dulcemayor.entity.order.PointOfSale;
import ing.software.dulcemayor.entity.org.CompanyEmailConfig;
import ing.software.dulcemayor.entity.org.EmailContact;
import ing.software.dulcemayor.entity.org.PhoneContact;
import ing.software.dulcemayor.entity.org.Wholesaler;
import ing.software.dulcemayor.enums.AuthProvider;
import ing.software.dulcemayor.enums.CompanyType;
import ing.software.dulcemayor.enums.ContactType;
import ing.software.dulcemayor.enums.DocumentType;
import ing.software.dulcemayor.enums.ImageType;
import ing.software.dulcemayor.enums.InvoiceStatus;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.enums.PaymentType;
import ing.software.dulcemayor.enums.PhoneType;
import ing.software.dulcemayor.enums.RouteStatus;
import ing.software.dulcemayor.enums.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EntityPersistenceIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Should successfully persist full domain model graph")
    void testPersistFullDomainGraph() {
        // 1. Location
        Country country = Country.builder().name("Argentina").build();
        entityManager.persist(country);

        State state = State.builder().name("Mendoza").country(country).build();
        entityManager.persist(state);

        Department department = Department.builder().name("Capital").state(state).build();
        entityManager.persist(department);

        City city = City.builder().name("Mendoza").postalCode("5500").department(department).build();
        entityManager.persist(city);

        Address address = Address.builder()
                .street("San Martin")
                .streetNumber("1020")
                .neighborhood("Centro")
                .city(city)
                .latitude(-32.889458)
                .longitude(-68.845839)
                .build();
        entityManager.persist(address);

        // 2. Media
        Image image = Image.builder()
                .name("alfajor_guaymallen.jpg")
                .mimeType("image/jpeg")
                .content(new byte[]{1, 2, 3})
                .imageType(ImageType.PRODUCT)
                .build();
        entityManager.persist(image);

        // 3. Org
        Wholesaler wholesaler = Wholesaler.builder()
                .businessName("Dulce Mayor S.A.")
                .taxId("30-71234567-8")
                .companyType(CompanyType.HEADQUARTERS)
                .address(address)
                .build();
        entityManager.persist(wholesaler);

        CompanyEmailConfig emailConfig = CompanyEmailConfig.builder()
                .email("info@dulcemayor.com")
                .password("secret123")
                .port("587")
                .smtpHost("smtp.mailtrap.io")
                .tls(true)
                .company(wholesaler)
                .build();
        entityManager.persist(emailConfig);

        // 4. Actors
        SalesRepresentative salesRep = SalesRepresentative.builder()
                .firstName("Juan")
                .lastName("Perez")
                .birthDate(LocalDate.of(1990, 5, 20))
                .documentType(DocumentType.DNI)
                .documentNumber("35123456")
                .employeeIdNumber("LEG-001")
                .build();
        entityManager.persist(salesRep);

        EmailContact repEmail = EmailContact.builder()
                .contactType(ContactType.WORK)
                .note("Work corporate mail")
                .email("juan.perez@dulcemayor.com")
                .person(salesRep)
                .build();
        entityManager.persist(repEmail);

        PhoneContact repPhone = PhoneContact.builder()
                .contactType(ContactType.WORK)
                .note("Mobile phone")
                .phoneNumber("+54 9 261 555-1234")
                .phoneType(PhoneType.MOBILE)
                .person(salesRep)
                .build();
        entityManager.persist(repPhone);

        DeliveryAgent deliveryAgent = DeliveryAgent.builder()
                .firstName("Carlos")
                .lastName("Gomez")
                .birthDate(LocalDate.of(1988, 3, 15))
                .documentType(DocumentType.DNI)
                .documentNumber("33987654")
                .employeeIdNumber("LEG-002")
                .build();
        entityManager.persist(deliveryAgent);

        Retailer retailer = Retailer.builder()
                .firstName("Maria")
                .lastName("Lopez")
                .birthDate(LocalDate.of(1985, 11, 10))
                .documentType(DocumentType.DNI)
                .documentNumber("31456789")
                .build();
        entityManager.persist(retailer);

        User user = User.builder()
                .username("juanperez")
                .password("$2a$10$encodedpassword")
                .role(UserRole.PRESALE_AGENT)
                .isTemporaryPassword(false)
                .authProvider(AuthProvider.LOCAL)
                .person(salesRep)
                .build();
        entityManager.persist(user);

        // 5. Catalog
        Category category = Category.builder().name("Alfajores").build();
        entityManager.persist(category);

        SubCategory subCategory = SubCategory.builder().name("Chocolate").category(category).build();
        entityManager.persist(subCategory);

        Product product = Product.builder()
                .code("ALF-CHO-01")
                .name("Alfajor Triple Chocolate")
                .description("Alfajor relleno con dulce de leche bañado en chocolate")
                .packaging("Pack x 12 unidades")
                .unitsPerPack(12)
                .isOnSale(true)
                .subCategory(subCategory)
                .image(image)
                .build();
        entityManager.persist(product);

        PriceValidity priceValidity = PriceValidity.builder()
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(null) // active price
                .price(15000.0)
                .product(product)
                .build();
        entityManager.persist(priceValidity);

        Stock stock = Stock.builder()
                .currentQuantity(250)
                .note("Available in central warehouse")
                .product(product)
                .build();
        entityManager.persist(stock);

        Promotion promo = Promotion.builder()
                .code("PROMO10")
                .description("10% off on Alfajores")
                .discountPercentage(10.0)
                .minimumQuantity(5)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(25))
                .product(product)
                .build();
        entityManager.persist(promo);

        // 6. Point of Sale & Order
        PointOfSale pos = PointOfSale.builder()
                .storeName("Kiosco Belgrano")
                .retailer(retailer)
                .address(address)
                .salesRep(salesRep)
                .build();
        entityManager.persist(pos);

        Order order = Order.builder()
                .orderNumber(1001L)
                .orderDateTime(LocalDateTime.now())
                .subtotal(30000.0)
                .discount(3000.0)
                .total(27000.0)
                .status(OrderStatus.LOADED)
                .pointOfSale(pos)
                .salesRep(salesRep)
                .couponCode("PROMO10")
                .build();
        entityManager.persist(order);

        OrderDetail orderDetail = OrderDetail.builder()
                .quantity(2)
                .unitPrice(15000.0)
                .subtotal(30000.0)
                .product(product)
                .order(order)
                .build();
        entityManager.persist(orderDetail);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .status(OrderStatus.LOADED)
                .dateTime(LocalDateTime.now())
                .note("Order registered by sales representative in field app")
                .user(user)
                .order(order)
                .build();
        entityManager.persist(history);

        // 7. Billing
        Invoice invoice = Invoice.builder()
                .invoiceNumber(2001L)
                .invoiceDate(LocalDateTime.now())
                .total(27000.0)
                .status(InvoiceStatus.PENDING_PAYMENT)
                .order(order)
                .build();
        entityManager.persist(invoice);

        InvoiceDetail invoiceDetail = InvoiceDetail.builder()
                .quantity(2)
                .unitPrice(15000.0)
                .subtotal(30000.0)
                .invoice(invoice)
                .build();
        entityManager.persist(invoiceDetail);

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .paymentType(PaymentType.DIGITAL_WALLET)
                .note("MercadoPago QR / Transferencia Alias: DULCE.MAYOR")
                .build();
        entityManager.persist(paymentMethod);

        Payment payment = Payment.builder()
                .amount(27000.0)
                .paymentDate(LocalDateTime.now())
                .reference("MP-TRANS-987654321")
                .note("Approved digital wallet payment")
                .invoice(invoice)
                .paymentMethod(paymentMethod)
                .build();
        entityManager.persist(payment);

        // 8. Logistics
        SalesRoute salesRoute = SalesRoute.builder()
                .date(LocalDate.now())
                .status(RouteStatus.PENDING)
                .currentStop(pos)
                .salesRep(salesRep)
                .build();
        entityManager.persist(salesRoute);

        DeliveryRoute deliveryRoute = DeliveryRoute.builder()
                .date(LocalDate.now().plusDays(1))
                .status(RouteStatus.PENDING)
                .currentStop(pos)
                .deliveryAgent(deliveryAgent)
                .build();
        entityManager.persist(deliveryRoute);

        entityManager.flush();

        // Assertions
        assertThat(country.getId()).isNotNull();
        assertThat(product.getId()).isNotNull();
        assertThat(order.getId()).isNotNull();
        assertThat(invoice.getId()).isNotNull();
        assertThat(payment.getId()).isNotNull();
        assertThat(salesRoute.getId()).isNotNull();
        assertThat(deliveryRoute.getId()).isNotNull();

        Country foundCountry = entityManager.find(Country.class, country.getId());
        assertThat(foundCountry).isNotNull();
        assertThat(foundCountry.getName()).isEqualTo("Argentina");
        assertThat(foundCountry.isDeleted()).isFalse();

        Product foundProduct = entityManager.find(Product.class, product.getId());
        assertThat(foundProduct).isNotNull();
        assertThat(foundProduct.getCode()).isEqualTo("ALF-CHO-01");

        Order foundOrder = entityManager.find(Order.class, order.getId());
        assertThat(foundOrder).isNotNull();
        assertThat(foundOrder.getOrderNumber()).isEqualTo(1001L);
        assertThat(foundOrder.getStatus()).isEqualTo(OrderStatus.LOADED);
    }
}
