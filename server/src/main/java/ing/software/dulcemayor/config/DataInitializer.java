package ing.software.dulcemayor.config;

import ing.software.dulcemayor.entity.actor.Employee;
import ing.software.dulcemayor.entity.actor.Retailer;
import ing.software.dulcemayor.entity.actor.SalesRepresentative;
import ing.software.dulcemayor.entity.actor.User;
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
import ing.software.dulcemayor.entity.order.OrderDetail;
import ing.software.dulcemayor.entity.order.PointOfSale;
import ing.software.dulcemayor.enums.AuthProvider;
import ing.software.dulcemayor.enums.DocumentType;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.enums.UserRole;
import ing.software.dulcemayor.repository.location.AddressRepository;
import ing.software.dulcemayor.repository.catalog.CategoryRepository;
import ing.software.dulcemayor.repository.location.CityRepository;
import ing.software.dulcemayor.repository.location.CountryRepository;
import ing.software.dulcemayor.repository.location.DepartmentRepository;
import ing.software.dulcemayor.repository.actor.EmployeeRepository;
import ing.software.dulcemayor.repository.order.OrderDetailRepository;
import ing.software.dulcemayor.repository.order.OrderRepository;
import ing.software.dulcemayor.repository.order.PointOfSaleRepository;
import ing.software.dulcemayor.repository.catalog.PriceValidityRepository;
import ing.software.dulcemayor.repository.catalog.ProductRepository;
import ing.software.dulcemayor.repository.actor.RetailerRepository;
import ing.software.dulcemayor.repository.actor.SalesRepresentativeRepository;
import ing.software.dulcemayor.repository.location.StateRepository;
import ing.software.dulcemayor.repository.catalog.StockRepository;
import ing.software.dulcemayor.repository.catalog.SubCategoryRepository;
import ing.software.dulcemayor.repository.actor.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@org.springframework.context.annotation.Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final SalesRepresentativeRepository salesRepRepository;
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
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        log.info("Seeding initial database data for Dulce Mayor...");

        // 1. Locations
        Country country = countryRepository.save(Country.builder().name("Argentina").build());
        State state = stateRepository.save(State.builder().name("Mendoza").country(country).build());
        Department department = departmentRepository.save(Department.builder().name("Capital").state(state).build());
        City city = cityRepository.save(City.builder().name("Mendoza").postalCode("5500").department(department).build());

        Address adminAddress = addressRepository.save(Address.builder()
                .street("Av. San Martín")
                .streetNumber("1500")
                .neighborhood("Centro")
                .city(city)
                .latitude(-32.89084)
                .longitude(-68.84583)
                .build());

        // 2. Administrative User (admin / dulcemayor)
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

        // 3. Sales Representative (Preventor)
        SalesRepresentative rep = salesRepRepository.save(SalesRepresentative.builder()
                .firstName("Juan")
                .lastName("Preventista")
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

        // 4. Retailer Customer
        Retailer retailer = retailerRepository.save(Retailer.builder()
                .firstName("Laura")
                .lastName("Comerciante")
                .birthDate(LocalDate.of(1987, 8, 24))
                .documentType(DocumentType.DNI)
                .documentNumber("33445566")
                .build());

        // 5. Point of Sale
        PointOfSale pos = pointOfSaleRepository.save(PointOfSale.builder()
                .storeName("Maxikiosco Los Hermanos")
                .retailer(retailer)
                .address(adminAddress)
                .salesRep(rep)
                .build());

        // 6. Categories & SubCategories
        Category catAlfajores = categoryRepository.save(Category.builder().name("Alfajores").build());
        Category catCaramelos = categoryRepository.save(Category.builder().name("Caramelos y Gomitas").build());
        Category catChocolates = categoryRepository.save(Category.builder().name("Chocolates").build());

        SubCategory subAlfajorTriple = subCategoryRepository.save(SubCategory.builder().name("Triples").category(catAlfajores).build());
        SubCategory subGomitas = subCategoryRepository.save(SubCategory.builder().name("Gomitas").category(catCaramelos).build());
        SubCategory subTabletas = subCategoryRepository.save(SubCategory.builder().name("Tabletas").category(catChocolates).build());

        // 7. Products
        Product p1 = createProduct("ALF-TRIP-CHO", "Alfajor Triple Chocolate", "Alfajor triple relleno de dulce de leche bañado en chocolate", "Pack x 12 u.", 12, false, subAlfajorTriple, 15000.0, 140);
        Product p2 = createProduct("GOM-FRUT-SURT", "Gomitas Frutales Surtidas", "Bolsa de gomitas frutales surtidas dulces", "Display x 24 u.", 24, true, subGomitas, 18500.0, 75);
        Product p3 = createProduct("CHO-ALM-100", "Chocolate con Almendras 100g", "Tableta de chocolate con leche y almendras enteras", "Pack x 10 u.", 10, false, subTabletas, 22000.0, 8); // LOW STOCK
        Product p4 = createProduct("CAR-ACID-MAST", "Caramelos Ácidos Masticables", "Caramelos masticables sabores ácidos frutales", "Bolsa x 50 u.", 50, false, subGomitas, 8000.0, 12); // LOW STOCK
        Product p5 = createProduct("ALF-BLAN-DOB", "Alfajor Blanco Doble", "Alfajor clásico bañado en chocolate blanco", "Pack x 12 u.", 12, true, subAlfajorTriple, 13200.0, 210);

        // 8. Sample Orders
        Order order1 = orderRepository.save(Order.builder()
                .orderNumber(1001L)
                .orderDateTime(LocalDateTime.now().minusHours(3))
                .subtotal(33500.0)
                .discount(0.0)
                .total(33500.0)
                .status(OrderStatus.LOADED)
                .pointOfSale(pos)
                .salesRep(rep)
                .build());

        orderDetailRepository.save(OrderDetail.builder().quantity(1).unitPrice(15000.0).subtotal(15000.0).product(p1).order(order1).build());
        orderDetailRepository.save(OrderDetail.builder().quantity(1).unitPrice(18500.0).subtotal(18500.0).product(p2).order(order1).build());

        Order order2 = orderRepository.save(Order.builder()
                .orderNumber(1002L)
                .orderDateTime(LocalDateTime.now().minusDays(1))
                .subtotal(30000.0)
                .discount(3000.0)
                .total(27000.0)
                .status(OrderStatus.INVOICED)
                .pointOfSale(pos)
                .salesRep(rep)
                .couponCode("BIENVENIDA")
                .build());

        orderDetailRepository.save(OrderDetail.builder().quantity(2).unitPrice(15000.0).subtotal(30000.0).product(p1).order(order2).build());

        log.info("Database successfully seeded with admin credentials and initial inventory!");
    }

    private Product createProduct(String code, String name, String desc, String pack, int units, boolean onSale, SubCategory subCat, double price, int stock) {
        Product product = productRepository.save(Product.builder()
                .code(code)
                .name(name)
                .description(desc)
                .packaging(pack)
                .unitsPerPack(units)
                .isOnSale(onSale)
                .subCategory(subCat)
                .build());

        priceValidityRepository.save(PriceValidity.builder()
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(null)
                .price(price)
                .product(product)
                .build());

        stockRepository.save(Stock.builder()
                .currentQuantity(stock)
                .note("Initial seed stock")
                .product(product)
                .build());

        return product;
    }
}
