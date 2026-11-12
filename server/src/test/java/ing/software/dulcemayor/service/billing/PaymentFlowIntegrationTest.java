package ing.software.dulcemayor.service.billing;

import ing.software.dulcemayor.entity.actor.DeliveryAgent;
import ing.software.dulcemayor.entity.actor.Retailer;
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
import ing.software.dulcemayor.repository.actor.DeliveryAgentRepository;
import ing.software.dulcemayor.repository.actor.RetailerRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:paymenttestdb;DB_CLOSE_DELAY=-1")
class PaymentFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
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

    @MockitoSpyBean
    private MercadoPagoService mercadoPagoService;

    private Product seededProduct;
    private PointOfSale seededPos;

    @BeforeEach
    void setUpFixtures() {
        Country country = countryRepository.save(Country.builder().name("Argentina").build());
        State state = stateRepository.save(State.builder().name("Mendoza").country(country).build());
        Department dept = departmentRepository.save(Department.builder().name("Capital").state(state).build());
        cityRepository.save(City.builder().name("Mendoza Capital").postalCode("5500").department(dept).build());
        City godoyCruz = cityRepository.save(City.builder().name("Godoy Cruz").postalCode("5501").department(dept).build());

        if (deliveryAgentRepository.findByIsDeletedFalse().isEmpty()) {
            deliveryAgentRepository.save(DeliveryAgent.builder()
                    .firstName("Carlos")
                    .lastName("Reparto")
                    .documentType(DocumentType.DNI)
                    .documentNumber("30-11111111-2")
                    .build());
        }

        Retailer retailer = retailerRepository.save(Retailer.builder()
                .firstName("Marta")
                .lastName("Villalobos")
                .documentType(DocumentType.DNI)
                .documentNumber("27-31298451-2")
                .build());
        Address addr = Address.builder()
                .street("Belgrano 840")
                .neighborhood("Bombal")
                .city(godoyCruz)
                .latitude(-32.9052)
                .longitude(-68.8431)
                .build();
        seededPos = pointOfSaleRepository.save(PointOfSale.builder()
                .storeName("Almacén Dulce Hogar " + System.nanoTime())
                .retailer(retailer)
                .address(addr)
                .build());

        Category cat = categoryRepository.save(Category.builder().name("Galletitas").build());
        SubCategory sub = subCategoryRepository.save(SubCategory.builder().name("Rellenas").category(cat).build());
        seededProduct = productRepository.save(Product.builder()
                .code("IT-" + System.nanoTime())
                .name("Oreo Original Test")
                .packaging("Caja x 28 paq.")
                .unitsPerPack(28)
                .subCategory(sub)
                .build());
        priceValidityRepository.save(PriceValidity.builder()
                .product(seededProduct)
                .price(33600.0)
                .startDate(LocalDate.now().minusDays(1))
                .build());
        stockRepository.save(Stock.builder()
                .product(seededProduct)
                .currentQuantity(80)
                .build());
    }

    @Test
    @DisplayName("Solicitud 1: Landing checkout with Mercado Pago -> PENDING_PAYMENT -> verified return -> invoice + stock + delivery route")
    void landingCheckoutMercadoPago_accreditsAndAppearsInDeliveryRoute() throws Exception {
        int initialStock = stockRepository.findByProductIdAndIsDeletedFalse(seededProduct.getId())
                .orElseThrow().getCurrentQuantity();

        doReturn(true).when(mercadoPagoService).isConfigured();
        doReturn(new MercadoPagoService.PreferenceResult(
                "PREF-LANDING-001",
                "https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=PREF-LANDING-001"
        )).when(mercadoPagoService).executeCreatePreference(any());

        String checkoutPayload = String.format("""
                {
                  "customerName": "Lucía Fernández",
                  "customerEmail": "lucia@kiosco.com",
                  "customerPhone": "261-555-8899",
                  "storeName": "Kiosco Sol Naciente",
                  "deliveryAddress": "Av. España 980",
                  "deliveryNeighborhood": "Centro",
                  "deliveryCity": "Mendoza Capital",
                  "paymentType": "MERCADO_PAGO",
                  "returnBaseUrl": "http://localhost:8081",
                  "items": [{ "productId": "%s", "quantity": 2 }]
                }
                """, seededProduct.getId());

        MvcResult createRes = mockMvc.perform(post("/api/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.preferenceId").value("PREF-LANDING-001"))
                .andExpect(jsonPath("$.data.initPoint").value("https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=PREF-LANDING-001"))
                .andExpect(jsonPath("$.data.readyForDispatch").value(false))
                .andReturn();

        JsonNode data = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("data");
        String orderId = data.get("orderId").asString();
        double total = data.get("total").asDouble();

        int stockWhilePending = stockRepository.findByProductIdAndIsDeletedFalse(seededProduct.getId())
                .orElseThrow().getCurrentQuantity();
        assertThat(stockWhilePending).isEqualTo(initialStock);

        String pendingRouteJson = mockMvc.perform(get("/api/routes/daily").param("role", "DELIVERY_AGENT"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(pendingRouteJson).doesNotContain(orderId);

        doReturn(Optional.of(new MercadoPagoService.MpPaymentInfo(
                991001L,
                "approved",
                "accredited",
                orderId,
                BigDecimal.valueOf(total)
        ))).when(mercadoPagoService).executeGetPayment(991001L);

        mockMvc.perform(get("/api/payments/mp/verify")
                        .param("orderId", orderId)
                        .param("payment_id", "991001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paid").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("PAID"))
                .andExpect(jsonPath("$.data.stockDeducted").value(true))
                .andExpect(jsonPath("$.data.readyForDispatch").value(true))
                .andExpect(jsonPath("$.data.invoiceNumber").isNumber());

        int stockAfterPaid = stockRepository.findByProductIdAndIsDeletedFalse(seededProduct.getId())
                .orElseThrow().getCurrentQuantity();
        assertThat(stockAfterPaid).isEqualTo(initialStock - 2);

        MvcResult routeRes = mockMvc.perform(get("/api/routes/daily").param("role", "DELIVERY_AGENT"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode stops = objectMapper.readTree(routeRes.getResponse().getContentAsString())
                .path("data").path("stops");
        JsonNode matchedStop = null;
        for (JsonNode stop : stops) {
            if (orderId.equals(stop.path("orderId").asString())) {
                matchedStop = stop;
                break;
            }
        }
        assertThat(matchedStop).isNotNull();
        assertThat(matchedStop.path("paid").asBoolean()).isTrue();
        assertThat(matchedStop.path("readyForDispatch").asBoolean()).isTrue();
        assertThat(matchedStop.path("sourceChannel").asString()).isEqualTo("LANDING");
    }

    @Test
    @DisplayName("Solicitud 2: Preventista QR order -> PENDING_PAYMENT with QR payload -> localhost search polling -> PAID & dispatch")
    void preventistaQrOrder_pollsSearchOnLocalhostAndAccredits() throws Exception {
        doReturn(true).when(mercadoPagoService).isConfigured();
        doReturn(new MercadoPagoService.PreferenceResult(
                "PREF-QR-002",
                "https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=PREF-QR-002"
        )).when(mercadoPagoService).executeCreatePreference(any());

        String orderPayload = String.format("""
                {
                  "pointOfSaleId": "%s",
                  "paymentType": "MERCADO_PAGO",
                  "items": [{ "productId": "%s", "quantity": 1 }]
                }
                """, seededPos.getId(), seededProduct.getId());

        MvcResult createRes = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.mpPreferenceId").value("PREF-QR-002"))
                .andExpect(jsonPath("$.data.mpInitPoint").value("https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=PREF-QR-002"))
                .andReturn();

        JsonNode data = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("data");
        String orderId = data.get("id").asString();
        double total = data.get("total").asDouble();
        assertThat(data.get("qrImageBase64").asString()).startsWith("data:image/png;base64,");

        mockMvc.perform(post("/api/orders/{id}/mp/qr", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.initPoint").value("https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=PREF-QR-002"))
                .andExpect(jsonPath("$.data.qrPayload").value("https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=PREF-QR-002"));

        doReturn(List.of()).when(mercadoPagoService).executeSearchPayments(any(String.class));
        mockMvc.perform(get("/api/payments/mp/status/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paid").value(false))
                .andExpect(jsonPath("$.data.orderStatus").value("PENDING_PAYMENT"));

        doReturn(List.of(new MercadoPagoService.MpPaymentInfo(
                992002L,
                "approved",
                "accredited",
                orderId,
                BigDecimal.valueOf(total)
        ))).when(mercadoPagoService).executeSearchPayments(orderId);

        mockMvc.perform(get("/api/payments/mp/status/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paid").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("PAID"))
                .andExpect(jsonPath("$.data.mpPaymentId").value("992002"))
                .andExpect(jsonPath("$.data.readyForDispatch").value(true))
                .andExpect(jsonPath("$.data.invoiceNumber").isNumber());
    }

    @Test
    @DisplayName("Webhook v2 & Bank Transfer manual confirm work idempotently and expose invoice")
    void webhookAndBankTransferManualConfirm_workCleanly() throws Exception {
        String transferPayload = String.format("""
                {
                  "customerName": "Marcos Díaz",
                  "customerEmail": "marcos@almacen.com",
                  "customerPhone": "261-400-1122",
                  "deliveryAddress": "Belgrano 440",
                  "deliveryCity": "Godoy Cruz",
                  "paymentType": "BANK_TRANSFER",
                  "items": [{ "productId": "%s", "quantity": 1 }]
                }
                """, seededProduct.getId());

        MvcResult transferRes = mockMvc.perform(post("/api/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.bankAlias").isNotEmpty())
                .andReturn();

        String transferOrderId = objectMapper.readTree(transferRes.getResponse().getContentAsString())
                .get("data").get("orderId").asString();

        mockMvc.perform(post("/api/payments/orders/{orderId}/confirm-manual", transferOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentType\":\"BANK_TRANSFER\",\"reference\":\"TRF-MZA-441\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paid").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("PAID"))
                .andExpect(jsonPath("$.data.readyForDispatch").value(true));

        mockMvc.perform(get("/api/payments/orders/{orderId}/invoice", transferOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentReference").value("TRF-MZA-441"))
                .andExpect(jsonPath("$.data.details.length()").value(1));

        int qtyBeforeWebhook = stockRepository.findByProductIdAndIsDeletedFalse(seededProduct.getId())
                .orElseThrow().getCurrentQuantity();

        doReturn(true).when(mercadoPagoService).isConfigured();
        doReturn(new MercadoPagoService.PreferenceResult(
                "PREF-WH-003",
                "https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=PREF-WH-003"
        )).when(mercadoPagoService).executeCreatePreference(any());

        MvcResult mpOrderRes = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"pointOfSaleId\":\"%s\",\"paymentType\":\"MERCADO_PAGO\",\"items\":[{\"productId\":\"%s\",\"quantity\":1}]}",
                                seededPos.getId(), seededProduct.getId())))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode mpOrderNode = objectMapper.readTree(mpOrderRes.getResponse().getContentAsString()).get("data");
        String mpOrderId = mpOrderNode.get("id").asString();
        double mpTotal = mpOrderNode.get("total").asDouble();

        doReturn(Optional.of(new MercadoPagoService.MpPaymentInfo(
                993003L,
                "approved",
                "accredited",
                mpOrderId,
                BigDecimal.valueOf(mpTotal)
        ))).when(mercadoPagoService).executeGetPayment(993003L);

        mockMvc.perform(post("/api/payments/mp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"payment\",\"data\":{\"id\":\"993003\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));

        mockMvc.perform(get("/api/payments/mp/webhook")
                        .param("topic", "payment")
                        .param("id", "993003"))
                .andExpect(status().isOk());

        Order accredited = orderRepository.findByIdAndIsDeletedFalse(mpOrderId).orElseThrow();
        assertThat(accredited.isStockDeducted()).isTrue();
        int qtyAfterWebhook = stockRepository.findByProductIdAndIsDeletedFalse(seededProduct.getId())
                .orElseThrow().getCurrentQuantity();
        assertThat(qtyAfterWebhook).isEqualTo(qtyBeforeWebhook - 1);
    }
}
