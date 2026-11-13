package ing.software.dulcemayor.config;

import ing.software.dulcemayor.entity.actor.DeliveryAgent;
import ing.software.dulcemayor.entity.actor.Employee;
import ing.software.dulcemayor.entity.actor.Retailer;
import ing.software.dulcemayor.entity.actor.SalesRepresentative;
import ing.software.dulcemayor.entity.actor.User;
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
import ing.software.dulcemayor.entity.billing.Invoice;
import ing.software.dulcemayor.entity.billing.InvoiceDetail;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.entity.order.OrderDetail;
import ing.software.dulcemayor.entity.order.PointOfSale;
import ing.software.dulcemayor.enums.AuthProvider;
import ing.software.dulcemayor.enums.DocumentType;
import ing.software.dulcemayor.enums.InvoiceStatus;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.enums.UserRole;
import ing.software.dulcemayor.repository.actor.DeliveryAgentRepository;
import ing.software.dulcemayor.repository.actor.EmployeeRepository;
import ing.software.dulcemayor.repository.actor.RetailerRepository;
import ing.software.dulcemayor.repository.actor.SalesRepresentativeRepository;
import ing.software.dulcemayor.repository.actor.UserRepository;
import ing.software.dulcemayor.repository.billing.InvoiceRepository;
import ing.software.dulcemayor.repository.catalog.CategoryRepository;
import ing.software.dulcemayor.repository.catalog.PriceValidityRepository;
import ing.software.dulcemayor.repository.catalog.ProductRepository;
import ing.software.dulcemayor.repository.catalog.PromotionRepository;
import ing.software.dulcemayor.repository.catalog.StockRepository;
import ing.software.dulcemayor.repository.catalog.SubCategoryRepository;
import ing.software.dulcemayor.repository.location.AddressRepository;
import ing.software.dulcemayor.repository.location.CityRepository;
import ing.software.dulcemayor.repository.location.CountryRepository;
import ing.software.dulcemayor.repository.location.DepartmentRepository;
import ing.software.dulcemayor.repository.location.StateRepository;
import ing.software.dulcemayor.repository.order.OrderDetailRepository;
import ing.software.dulcemayor.repository.order.OrderRepository;
import ing.software.dulcemayor.repository.order.PointOfSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Slf4j
@Component
@org.springframework.context.annotation.Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

        private final UserRepository userRepository;
        private final EmployeeRepository employeeRepository;
        private final SalesRepresentativeRepository salesRepRepository;
        private final DeliveryAgentRepository deliveryAgentRepository;
        private final RetailerRepository retailerRepository;
        private final CountryRepository countryRepository;
        private final StateRepository stateRepository;
        private final DepartmentRepository departmentRepository;
        private final CityRepository cityRepository;
        private final AddressRepository addressRepository;
        private final CategoryRepository categoryRepository;
        private final SubCategoryRepository subCategoryRepository;
        private final ProductRepository productRepository;
        private final PriceValidityRepository priceValidityRepository;
        private final StockRepository stockRepository;
        private final PointOfSaleRepository pointOfSaleRepository;
        private final OrderRepository orderRepository;
        private final OrderDetailRepository orderDetailRepository;
        private final InvoiceRepository invoiceRepository;
        private final PromotionRepository promotionRepository;
        private final PasswordEncoder passwordEncoder;
        private final ing.software.dulcemayor.service.catalog.ExchangeRateService exchangeRateService;
        private final ing.software.dulcemayor.service.billing.factory.InvoiceFactoryProvider invoiceFactoryProvider;

        @Override
        @Transactional
        public void run(String... args) {
                if (userRepository.count() > 0) {
                        return;
                }

                log.info("Seeding initial database data for Dulce Mayor..."); // Valores iniciales de prueba, carga
                                                                              // inicial.

                // 1. Locations (Greater Mendoza)
                Country country = countryRepository.save(Country.builder().name("Argentina").build());
                State state = stateRepository.save(State.builder().name("Mendoza").country(country).build());
                Department depCapital = departmentRepository
                                .save(Department.builder().name("Capital").state(state).build());
                Department depGodoyCruz = departmentRepository
                                .save(Department.builder().name("Godoy Cruz").state(state).build());
                Department depGuaymallen = departmentRepository
                                .save(Department.builder().name("Guaymallén").state(state).build());
                Department depMaipu = departmentRepository
                                .save(Department.builder().name("Maipú").state(state).build());
                Department depLasHeras = departmentRepository
                                .save(Department.builder().name("Las Heras").state(state).build());

                City cityCapital = cityRepository.save(City.builder().name("Mendoza Capital").postalCode("5500")
                                .department(depCapital).build());
                City cityGodoyCruz = cityRepository.save(
                                City.builder().name("Godoy Cruz").postalCode("5501").department(depGodoyCruz).build());
                City cityGuaymallen = cityRepository.save(
                                City.builder().name("Guaymallén").postalCode("5519").department(depGuaymallen).build());
                City cityMaipu = cityRepository
                                .save(City.builder().name("Maipú").postalCode("5515").department(depMaipu).build());
                City cityLasHeras = cityRepository.save(
                                City.builder().name("Las Heras").postalCode("5539").department(depLasHeras).build());

                // 2. Users & Employees
                // Admin
                Employee adminEmployee = employeeRepository.save(Employee.builder()
                                .firstName("Jefe")
                                .lastName("Administrativo")
                                .birthDate(LocalDate.of(1980, 1, 1))
                                .documentType(DocumentType.DNI)
                                .documentNumber("28123456")
                                .employeeIdNumber("ADM-001")
                                .build());

                userRepository.save(User.builder()
                                .username("admin")
                                .password(passwordEncoder.encode("dulcemayor"))
                                .role(UserRole.MANAGER)
                                .isTemporaryPassword(false)
                                .authProvider(AuthProvider.LOCAL)
                                .person(adminEmployee)
                                .build());

                // Sales Rep (Preventor)
                SalesRepresentative rep = salesRepRepository.save(SalesRepresentative.builder()
                                .firstName("Juan Manuel")
                                .lastName("Torres")
                                .birthDate(LocalDate.of(1992, 4, 12))
                                .documentType(DocumentType.DNI)
                                .documentNumber("36789123")
                                .employeeIdNumber("PREV-010")
                                .build());

                userRepository.save(User.builder()
                                .username("preventor")
                                .password(passwordEncoder.encode("dulcemayor"))
                                .role(UserRole.PRESALE_AGENT)
                                .isTemporaryPassword(false)
                                .authProvider(AuthProvider.LOCAL)
                                .person(rep)
                                .build());

                // Delivery Agent (Repartidor)
                DeliveryAgent deliveryAgent = deliveryAgentRepository.save(DeliveryAgent.builder()
                                .firstName("Carlos")
                                .lastName("Mendoza")
                                .birthDate(LocalDate.of(1989, 7, 19))
                                .documentType(DocumentType.DNI)
                                .documentNumber("34567890")
                                .employeeIdNumber("REP-005")
                                .build());

                userRepository.save(User.builder()
                                .username("repartidor")
                                .password(passwordEncoder.encode("dulcemayor"))
                                .role(UserRole.DELIVERY_AGENT)
                                .isTemporaryPassword(false)
                                .authProvider(AuthProvider.LOCAL)
                                .person(deliveryAgent)
                                .build());

                // 3. Retailers & Points of Sale (5 across Greater Mendoza)
                Retailer r1 = retailerRepository.save(Retailer.builder().firstName("Roberto").lastName("Gómez")
                                .documentType(DocumentType.DNI).documentNumber("20-28491823-4")
                                .phone("+54 9 261 420-1122").build());
                Address a1 = addressRepository
                                .save(Address.builder().street("Av. San Martín 1420").neighborhood("Centro")
                                                .reference("Horario de descarga: 08:30 a 13:30 hs")
                                                .city(cityCapital).latitude(-32.8895).longitude(-68.8458).build());
                PointOfSale pos1 = pointOfSaleRepository.save(PointOfSale.builder().storeName("Kiosco El Trébol")
                                .contactName("Roberto Gómez")
                                .contactPhone("+54 9 261 420-1122")
                                .contactEmail("cliente@dulcemayor.com.ar")
                                .taxIdOrDni("20-28491823-4")
                                .deliveryNotes("Horario de descarga: 08:30 a 13:30 hs")
                                .retailer(r1).address(a1).salesRep(rep).build());

                userRepository.save(User.builder()
                                .username("cliente@dulcemayor.com.ar")
                                .password(passwordEncoder.encode("cliente123"))
                                .role(UserRole.RETAILER)
                                .isTemporaryPassword(false)
                                .authProvider(AuthProvider.LOCAL)
                                .person(r1)
                                .build());

                Retailer r2 = retailerRepository.save(Retailer.builder().firstName("Marta").lastName("Villalobos")
                                .documentType(DocumentType.DNI).documentNumber("27-31298451-2").build());
                Address a2 = addressRepository.save(Address.builder().street("Belgrano 840").neighborhood("Bombal")
                                .city(cityGodoyCruz).latitude(-32.9052).longitude(-68.8431).build());
                PointOfSale pos2 = pointOfSaleRepository.save(PointOfSale.builder().storeName("Almacén Dulce Hogar")
                                .retailer(r2).address(a2).salesRep(rep).build());

                Retailer r3 = retailerRepository.save(Retailer.builder().firstName("Esteban").lastName("Paz")
                                .documentType(DocumentType.DNI).documentNumber("20-33418729-1").build());
                Address a3 = addressRepository
                                .save(Address.builder().street("Bandera de los Andes 2150").neighborhood("San José")
                                                .city(cityGuaymallen).latitude(-32.8942).longitude(-68.8189).build());
                PointOfSale pos3 = pointOfSaleRepository
                                .save(PointOfSale.builder().storeName("Golosinas & Kiosco Central").retailer(r3)
                                                .address(a3).salesRep(rep).build());

                Retailer r4 = retailerRepository.save(Retailer.builder().firstName("Carla").lastName("Ferreyra")
                                .documentType(DocumentType.DNI).documentNumber("27-35619482-9").build());
                Address a4 = addressRepository.save(Address.builder().street("Pescara 350").neighborhood("Centro")
                                .city(cityMaipu).latitude(-32.9814).longitude(-68.7845).build());
                PointOfSale pos4 = pointOfSaleRepository.save(PointOfSale.builder()
                                .storeName("Kiosco 24 Horas La Plaza").retailer(r4).address(a4).salesRep(rep).build());

                Retailer r5 = retailerRepository.save(Retailer.builder().firstName("Luciano").lastName("Rossi")
                                .documentType(DocumentType.DNI).documentNumber("20-29841720-3").build());
                Address a5 = addressRepository.save(Address.builder().street("San Miguel 1120").neighborhood("Centro")
                                .city(cityLasHeras).latitude(-32.8521).longitude(-68.8312).build());
                PointOfSale pos5 = pointOfSaleRepository.save(PointOfSale.builder().storeName("Kiosco El Chiche")
                                .retailer(r5).address(a5).salesRep(rep).build());

                // 4. Categories & SubCategories
                Category catChocolates = categoryRepository
                                .save(Category.builder().name("Bombones y Chocolates").build());
                Category catGalletitas = categoryRepository
                                .save(Category.builder().name("Galletitas y Obleas").build());
                Category catGomitas = categoryRepository.save(Category.builder().name("Gomitas y Caramelos").build());
                Category catChicles = categoryRepository.save(Category.builder().name("Chicles y Pastillas").build());

                SubCategory subBombones = subCategoryRepository
                                .save(SubCategory.builder().name("Bombones").category(catChocolates).build());
                SubCategory subTabletas = subCategoryRepository
                                .save(SubCategory.builder().name("Tabletas").category(catChocolates).build());
                SubCategory subGalletitas = subCategoryRepository
                                .save(SubCategory.builder().name("Galletitas").category(catGalletitas).build());
                SubCategory subObleas = subCategoryRepository
                                .save(SubCategory.builder().name("Obleas").category(catGalletitas).build());
                SubCategory subGomitasSub = subCategoryRepository
                                .save(SubCategory.builder().name("Gomitas").category(catGomitas).build());
                SubCategory subCaramelos = subCategoryRepository
                                .save(SubCategory.builder().name("Caramelos").category(catGomitas).build());
                SubCategory subChiclesSub = subCategoryRepository
                                .save(SubCategory.builder().name("Chicles").category(catChicles).build());

                // 5. Authentic FMCG Confectionery Products (Arcor, Bagley, Mondelez, Terrabusi)
                Product p1 = createProduct("ARC-BON-01", "Bon o Bon Leche",
                                "Bombón con relleno de pasta de maní bañado en chocolate de leche.", "Display x 24 u.",
                                24, true, subBombones, 14200.0, 180,
                                "https://images.openfoodfacts.org/images/products/000/007/795/8921/front_es.43.400.jpg");
                Product p2 = createProduct("BAG-SUR-02", "Surtido Bagley Clásico",
                                "Pack familiar surtido con las tradicionales galletitas Bagley surtidas.",
                                "Caja x 15 paq. (390g)", 15, false, subGalletitas, 22500.0, 95,
                                "https://images.openfoodfacts.org/images/products/779/004/014/3357/front_en.3.400.jpg");
                Product p3 = createProduct("MDZ-ORE-03", "Oreo Original Regular",
                                "Galletitas rellenas de crema con sabor a vainilla.", "Caja x 28 paq. (118g)", 28, true,
                                subGalletitas, 33600.0, 140,
                                "https://images.openfoodfacts.org/images/products/200/000/004/6692/front_en.3.400.jpg");
                Product p4 = createProduct("ARC-MOG-04", "Mogul Gomitas Frutales",
                                "Gomitas sabor frutas surtidas libres de gluten.", "Display x 12 bolsas (150g)", 12,
                                false, subGomitasSub, 12800.0, 210,
                                "https://images.openfoodfacts.org/images/products/779/058/019/9906/front_es.4.400.jpg");
                Product p5 = createProduct("MDZ-MIL-05", "Milka Chocolate con Leche 100g",
                                "Auténtico chocolate suizo con leche de los Alpes.", "Display x 16 tabletas", 16, true,
                                subTabletas, 28800.0, 65,
                                "https://images.openfoodfacts.org/images/products/762/220/112/5813/front_en.3.400.jpg");
                Product p6 = createProduct("TER-RHOD-06", "Rhodesia Oblea Bañada",
                                "Oblea rellena con crema sabor limón bañada con chocolate con leche.",
                                "Display x 36 u.", 36, false, subObleas, 19800.0, 120,
                                "https://images.openfoodfacts.org/images/products/000/007/799/5681/front_es.41.400.jpg");
                Product p7 = createProduct("ARC-TOP-07", "Topline Menta Fuerte",
                                "Chicles sabor menta fuerte sin azúcar de máxima frescura.", "Display x 20 paq.", 20,
                                false, subChiclesSub, 11000.0, 340,
                                "https://images.openfoodfacts.org/images/products/779/058/012/0702/front_es.9.400.jpg");
                Product p8 = createProduct("TER-TITA-08", "Tita Galletita Bañada con Limón",
                                "Galletita crocante rellena de limón con cobertura chocolatosa.", "Display x 36 u.", 36,
                                false, subObleas, 18000.0, 15,
                                "https://images.openfoodfacts.org/images/products/000/007/797/6291/front_es.38.400.jpg"); // LOW
                                                                                                                                 // STOCK
                Product p9 = createProduct("BAG-CHO-09", "Chocolinas Clásicas 250g",
                                "Galletitas dulces con sabor a chocolate ideales para chocotorta.", "Caja x 20 paq.",
                                20, false, subGalletitas, 26000.0, 8,
                                "https://images.openfoodfacts.org/images/products/779/004/014/3234/front_es.3.400.jpg"); // LOW
                                                                                                                                 // STOCK
                Product p10 = createProduct("ARC-COF-10", "Cofler Block Chocolate 300g",
                                "Chocolate con leche y maní tostado seleccionado.", "Pack x 10 u.", 10, true,
                                subTabletas, 35000.0, 55,
                                "https://images.openfoodfacts.org/images/products/000/007/795/3124/front_es.35.400.jpg");
                Product p11 = createProduct("MDZ-BEL-11", "Beldent Infinit Menta",
                                "Chicles sin azúcar extra duración sabor menta fresca.", "Display x 18 paq.", 18, false,
                                subChiclesSub, 16200.0, 190,
                                "https://images.openfoodfacts.org/images/products/762/220/145/7334/front_es.8.400.jpg");
                Product p12 = createProduct("ARC-ROC-12", "Rocklets Chocolate Confitado",
                                "Pastillas confitadas con centro de rico chocolate con leche.", "Display x 24 tubos",
                                24, true, subBombones, 21600.0, 110,
                                "https://images.openfoodfacts.org/images/products/779/058/032/7415/front_es.27.400.jpg");
                Product p13 = createProduct("BAG-SON-13", "Sonrisas Frambuesa 118g",
                                "Galletitas dulces con jalea sabor frambuesa.", "Caja x 24 paq.", 24, false,
                                subGalletitas, 24000.0, 14,
                                "https://images.openfoodfacts.org/images/products/779/004/013/3488/front_en.4.400.jpg"); // LOW
                                                                                                                                 // STOCK
                Product p14 = createProduct("BAG-CRI-14", "Criollitas Galletitas de Agua",
                                "Galletitas crocantes de agua tradicionales.", "Caja x 30 paq.", 30, false,
                                subGalletitas, 27000.0, 85,
                                "https://images.openfoodfacts.org/images/products/779/004/037/7806/front_es.34.400.jpg");
                Product p15 = createProduct("TER-VAR-15", "Variedad Terrabusi 400g",
                                "Surtido dulce de galletitas Terrabusi.", "Caja x 14 paq.", 14, false, subGalletitas,
                                23800.0, 70,
                                "https://images.openfoodfacts.org/images/products/076/777/803/0430/front_en.3.400.jpg");

                // 6. Orders
                Order o1 = orderRepository.save(Order.builder()
                                .orderNumber(1001L)
                                .orderDateTime(LocalDateTime.now().minusHours(4))
                                .subtotal(47800.0)
                                .discount(0.0)
                                .total(47800.0)
                                .status(OrderStatus.DELIVERED)
                                .paymentType(ing.software.dulcemayor.enums.PaymentType.MERCADO_PAGO)
                                .customerName("Roberto Gómez")
                                .customerEmail("cliente@dulcemayor.com.ar")
                                .customerPhone("+54 9 261 420-1122")
                                .deliveryAddress("Av. San Martín 1420")
                                .deliveryNeighborhood("Centro")
                                .deliveryCity("Capital")
                                .pointOfSale(pos1)
                                .salesRep(rep)
                                .orderDetails(new ArrayList<>())
                                .build());
                OrderDetail od1a = orderDetailRepository.save(OrderDetail.builder().quantity(2).unitPrice(14200.0).subtotal(28400.0)
                                .product(p1).order(o1).build());
                OrderDetail od1b = orderDetailRepository.save(OrderDetail.builder().quantity(1).unitPrice(19800.0).subtotal(19800.0)
                                .product(p6).order(o1).build());
                o1.getOrderDetails().add(od1a);
                o1.getOrderDetails().add(od1b);

                Invoice inv1 = invoiceFactoryProvider.createInvoiceForOrder(o1, 10001L);
                inv1.setInvoiceDate(o1.getOrderDateTime().plusMinutes(10));
                invoiceRepository.save(inv1);

                Order o2 = orderRepository.save(Order.builder()
                                .orderNumber(1002L)
                                .orderDateTime(LocalDateTime.now().minusHours(2))
                                .subtotal(33600.0)
                                .discount(0.0)
                                .total(33600.0)
                                .status(OrderStatus.LOADED)
                                .paymentType(ing.software.dulcemayor.enums.PaymentType.CASH)
                                .pointOfSale(pos2)
                                .salesRep(rep)
                                .orderDetails(new ArrayList<>())
                                .build());
                orderDetailRepository.save(OrderDetail.builder().quantity(1).unitPrice(33600.0).subtotal(33600.0)
                                .product(p3).order(o2).build());

                Order o3 = orderRepository.save(Order.builder()
                                .orderNumber(1003L)
                                .orderDateTime(LocalDateTime.now().minusDays(1))
                                .subtotal(30000.0)
                                .discount(3000.0)
                                .total(27000.0)
                                .status(OrderStatus.INVOICED)
                                .paymentType(ing.software.dulcemayor.enums.PaymentType.BANK_TRANSFER)
                                .pointOfSale(pos3)
                                .salesRep(rep)
                                .couponCode("BIENVENIDA")
                                .orderDetails(new ArrayList<>())
                                .build());
                OrderDetail od3 = orderDetailRepository.save(OrderDetail.builder().quantity(2).unitPrice(15000.0).subtotal(30000.0)
                                .product(p1).order(o3).build());
                o3.getOrderDetails().add(od3);

                Invoice inv3 = invoiceFactoryProvider.createInvoiceForOrder(o3, 10002L);
                inv3.setInvoiceDate(o3.getOrderDateTime().plusMinutes(25));
                invoiceRepository.save(inv3);

                Order o4 = orderRepository.save(Order.builder()
                                .orderNumber(1004L)
                                .orderDateTime(LocalDateTime.now().minusHours(1))
                                .subtotal(81000.0)
                                .discount(0.0)
                                .total(81000.0)
                                .status(OrderStatus.LOADED)
                                .paymentType(ing.software.dulcemayor.enums.PaymentType.CASH)
                                .pointOfSale(pos4)
                                .salesRep(rep)
                                .orderDetails(new ArrayList<>())
                                .build());
                orderDetailRepository.save(OrderDetail.builder().quantity(2).unitPrice(35000.0).subtotal(70000.0)
                                .product(p10).order(o4).build());
                orderDetailRepository.save(OrderDetail.builder().quantity(1).unitPrice(11000.0).subtotal(11000.0)
                                .product(p7).order(o4).build());

                Order o5 = orderRepository.save(Order.builder()
                                .orderNumber(998L)
                                .orderDateTime(LocalDateTime.now().minusDays(4).minusHours(3))
                                .subtotal(67500.0)
                                .discount(0.0)
                                .total(67500.0)
                                .status(OrderStatus.DELIVERED)
                                .paymentType(ing.software.dulcemayor.enums.PaymentType.MERCADO_PAGO)
                                .pointOfSale(pos2)
                                .salesRep(rep)
                                .stockDeducted(true)
                                .readyForDispatch(true)
                                .orderDetails(new ArrayList<>())
                                .build());
                OrderDetail od5 = orderDetailRepository.save(OrderDetail.builder().quantity(3).unitPrice(22500.0).subtotal(67500.0)
                                .product(p2).order(o5).build());
                o5.getOrderDetails().add(od5);
                Invoice inv5 = invoiceFactoryProvider.createInvoiceForOrder(o5, 10003L);
                inv5.setInvoiceDate(o5.getOrderDateTime().plusMinutes(15));
                invoiceRepository.save(inv5);

                Order o6 = orderRepository.save(Order.builder()
                                .orderNumber(999L)
                                .orderDateTime(LocalDateTime.now().minusDays(3).minusHours(2))
                                .subtotal(57600.0)
                                .discount(0.0)
                                .total(57600.0)
                                .status(OrderStatus.DELIVERED)
                                .paymentType(ing.software.dulcemayor.enums.PaymentType.BANK_TRANSFER)
                                .pointOfSale(pos5)
                                .salesRep(rep)
                                .stockDeducted(true)
                                .readyForDispatch(true)
                                .orderDetails(new ArrayList<>())
                                .build());
                OrderDetail od6 = orderDetailRepository.save(OrderDetail.builder().quantity(2).unitPrice(28800.0).subtotal(57600.0)
                                .product(p5).order(o6).build());
                o6.getOrderDetails().add(od6);
                Invoice inv6 = invoiceFactoryProvider.createInvoiceForOrder(o6, 10004L);
                inv6.setInvoiceDate(o6.getOrderDateTime().plusMinutes(20));
                invoiceRepository.save(inv6);

                Order o7 = orderRepository.save(Order.builder()
                                .orderNumber(1000L)
                                .orderDateTime(LocalDateTime.now().minusDays(2).minusHours(5))
                                .subtotal(64800.0)
                                .discount(0.0)
                                .total(64800.0)
                                .status(OrderStatus.IN_DELIVERY)
                                .paymentType(ing.software.dulcemayor.enums.PaymentType.DIGITAL_WALLET)
                                .pointOfSale(pos1)
                                .salesRep(rep)
                                .stockDeducted(true)
                                .readyForDispatch(true)
                                .orderDetails(new ArrayList<>())
                                .build());
                OrderDetail od7 = orderDetailRepository.save(OrderDetail.builder().quantity(3).unitPrice(21600.0).subtotal(64800.0)
                                .product(p12).order(o7).build());
                o7.getOrderDetails().add(od7);
                Invoice inv7 = invoiceFactoryProvider.createInvoiceForOrder(o7, 10005L);
                inv7.setInvoiceDate(o7.getOrderDateTime().plusMinutes(12));
                invoiceRepository.save(inv7);

                // 7. Promotions
                promotionRepository.save(Promotion.builder()
                                .code("BIENVENIDA")
                                .description("10% de descuento en la primera compra mayorista")
                                .discountPercentage(10.0)
                                .minimumQuantity(1)
                                .startDate(LocalDate.now().minusDays(30))
                                .endDate(LocalDate.now().plusMonths(6))
                                .build());

                promotionRepository.save(Promotion.builder()
                                .code("BIENVENIDA10")
                                .description("10% de descuento en la primera compra mayorista")
                                .discountPercentage(10.0)
                                .minimumQuantity(1)
                                .startDate(LocalDate.now().minusDays(30))
                                .endDate(LocalDate.now().plusMonths(6))
                                .build());

                promotionRepository.save(Promotion.builder()
                                .code("ALFAJORFEST")
                                .description("15% off especial en compras por bulto de Bon o Bon")
                                .discountPercentage(15.0)
                                .minimumQuantity(2)
                                .startDate(LocalDate.now().minusDays(10))
                                .endDate(LocalDate.now().plusMonths(2))
                                .product(p1)
                                .build());

                promotionRepository.save(Promotion.builder()
                                .code("MAYORISTA2026")
                                .description("20% off en pedidos de más de 3 bultos de Chocolates")
                                .discountPercentage(20.0)
                                .minimumQuantity(3)
                                .startDate(LocalDate.now().minusDays(5))
                                .endDate(LocalDate.now().plusMonths(3))
                                .build());

                // 8. Synchronize initial catalog prices with live DolarApi / DolarHoy exchange rate
                try {
                        exchangeRateService.getCurrentQuoteAndEnsureSync(true);
                } catch (Exception e) {
                        log.warn("Could not synchronize live dollar rate during startup seed: {}", e.getMessage());
                }

                log.info("Database successfully seeded with 15 products, historical price validities, 5 Mendoza POS, 7 sample orders, and promotions!");
        }

        private Product createProduct(String code, String name, String desc, String pack, int units, boolean onSale,
                        SubCategory subCat, double price, int stock, String imageUrl) {
                double baseUsd = Math.round((price / 1080.0) * 100.0) / 100.0;

                Product product = productRepository.save(Product.builder()
                                .code(code)
                                .name(name)
                                .description(desc)
                                .packaging(pack)
                                .unitsPerPack(units)
                                .isOnSale(onSale)
                                .subCategory(subCat)
                                .imageUrl(imageUrl)
                                .basePriceUsd(baseUsd)
                                .build());

                // Historical price validity 1: 60 to 30 days ago (USD @ $960)
                double price60d = Math.round((baseUsd * 960.0) / 10.0) * 10.0;
                priceValidityRepository.save(PriceValidity.builder()
                                .startDate(LocalDate.now().minusDays(60))
                                .endDate(LocalDate.now().minusDays(30))
                                .price(price60d)
                                .usdPrice(baseUsd)
                                .exchangeRate(960.0)
                                .reason("Lista Mayorista Bimestral (Dólar Oficial $960)")
                                .product(product)
                                .build());

                // Historical price validity 2: 30 to 5 days ago (USD @ $1.020)
                double price30d = Math.round((baseUsd * 1020.0) / 10.0) * 10.0;
                priceValidityRepository.save(PriceValidity.builder()
                                .startDate(LocalDate.now().minusDays(30))
                                .endDate(LocalDate.now().minusDays(5))
                                .price(price30d)
                                .usdPrice(baseUsd)
                                .exchangeRate(1020.0)
                                .reason("Ajuste Cambiario Mensual (Dólar Oficial $1.020)")
                                .product(product)
                                .build());

                // Active price validity before live API sync (5 days ago -> present, USD @ $1.080)
                priceValidityRepository.save(PriceValidity.builder()
                                .startDate(LocalDate.now().minusDays(5))
                                .endDate(null)
                                .price(price)
                                .usdPrice(baseUsd)
                                .exchangeRate(1080.0)
                                .reason("Actualización Lista Mayorista (Dólar Oficial $1.080)")
                                .product(product)
                                .build());

                stockRepository.save(Stock.builder()
                                .currentQuantity(stock)
                                .note("Initial seed inventory")
                                .product(product)
                                .build());

                return product;
        }
}
