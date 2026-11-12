package ing.software.dulcemayor.service.billing;

import ing.software.dulcemayor.entity.catalog.Product;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.entity.order.OrderDetail;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.enums.PaymentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MercadoPagoServiceTest {

    private MercadoPagoService mercadoPagoService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(MercadoPagoService.MP_API_BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        mercadoPagoService = new MercadoPagoService(builder);
        ReflectionTestUtils.setField(mercadoPagoService, "accessToken", "TEST-123456-fake-token");
        ReflectionTestUtils.setField(mercadoPagoService, "publicKey", "TEST-pk-fake");
        ReflectionTestUtils.setField(mercadoPagoService, "sandboxEnabled", true);
        ReflectionTestUtils.setField(mercadoPagoService, "backendBaseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(mercadoPagoService, "frontendUrl", "http://localhost:8081");
    }

    @Test
    @DisplayName("REST preference on localhost: omits payer.email, auto_return and notification_url, and reads init_point")
    void createPreference_localhost_sendsExpectedHttpRequestAndReadsInitPoint() {
        Order order = sampleOrder("ord-uuid-1", 1005L, 28400.0, 0.0, 28400.0, "Juan Pérez");

        mockServer.expect(requestTo("https://api.mercadopago.com/checkout/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer TEST-123456-fake-token"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, Matchers.containsString("application/json")))
                .andExpect(jsonPath("$.external_reference").value("ord-uuid-1"))
                .andExpect(jsonPath("$.payer.name").value("Juan"))
                .andExpect(jsonPath("$.payer.surname").value("Pérez"))
                .andExpect(jsonPath("$.payer.email").doesNotExist())
                .andExpect(jsonPath("$.auto_return").doesNotExist())
                .andExpect(jsonPath("$.notification_url").doesNotExist())
                .andExpect(jsonPath("$.back_urls.success").value("http://localhost:8081/checkout/success?orderId=ord-uuid-1"))
                .andExpect(jsonPath("$.items[0].id").value("prod-1"))
                .andExpect(jsonPath("$.items[0].title").value("Bon o Bon Leche"))
                .andExpect(jsonPath("$.items[0].description").value("Display x 24 u."))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].currency_id").value("ARS"))
                .andRespond(withSuccess("""
                        {
                          "id": "PREF-987654",
                          "init_point": "https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=PREF-987654",
                          "sandbox_init_point": "https://sandbox.mercadopago.com.ar/broken"
                        }
                        """, MediaType.APPLICATION_JSON));

        MercadoPagoService.PreferenceResult result = mercadoPagoService.createPreference(order, "http://localhost:8081");

        assertThat(result.preferenceId()).isEqualTo("PREF-987654");
        assertThat(result.initPoint())
                .isEqualTo("https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=PREF-987654");
        mockServer.verify();
    }

    @Test
    @DisplayName("REST preference on public HTTPS: enables auto_return='approved' and webhook notification_url")
    void buildPreferencePayload_publicHttps_setsAutoReturnAndWebhook() {
        ReflectionTestUtils.setField(mercadoPagoService, "backendBaseUrl", "https://api.dulcemayor.com.ar");
        Order order = sampleOrder("ord-uuid-2", 1006L, 28400.0, 0.0, 28400.0, "Carla Ferreyra");

        Map<String, Object> payload = mercadoPagoService.buildPreferencePayload(order, "https://tienda.dulcemayor.com.ar");

        assertThat(payload.get("auto_return")).isEqualTo("approved");
        assertThat(payload.get("notification_url")).isEqualTo("https://api.dulcemayor.com.ar/api/payments/mp/webhook");
        @SuppressWarnings("unchecked")
        Map<String, Object> backUrls = (Map<String, Object>) payload.get("back_urls");
        assertThat(backUrls.get("success")).isEqualTo("https://tienda.dulcemayor.com.ar/checkout/success?orderId=ord-uuid-2");
    }

    @Test
    @DisplayName("coupon discount scales preference item prices cleanly to match order.getTotal()")
    void buildPreferencePayload_withCouponDiscount_matchesTotal() {
        Order order = sampleOrder("ord-uuid-4", 1008L, 28400.0, 2840.0, 25560.0, "Marta Villalobos");

        Map<String, Object> payload = mercadoPagoService.buildPreferencePayload(order, null);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

        BigDecimal mpTotal = items.stream()
                .map(i -> ((BigDecimal) i.get("unit_price"))
                        .multiply(BigDecimal.valueOf((Integer) i.get("quantity"))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(mpTotal).isEqualByComparingTo(BigDecimal.valueOf(25560.00));
    }

    @Test
    @DisplayName("REST GET /v1/payments/{id} and /v1/payments/search approve valid match and reject mismatches")
    void verifyAndSearchPayments_viaDirectRestApi() {
        Order order = sampleOrder("ord-uuid-5", 1009L, 28400.0, 0.0, 28400.0, "Esteban Paz");

        mockServer.expect(requestTo("https://api.mercadopago.com/v1/payments/555001"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer TEST-123456-fake-token"))
                .andRespond(withSuccess("""
                        {
                          "id": 555001,
                          "status": "approved",
                          "status_detail": "accredited",
                          "external_reference": "ord-uuid-5",
                          "transaction_amount": 28400.00
                        }
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://api.mercadopago.com/v1/payments/555002"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer TEST-123456-fake-token"))
                .andRespond(withSuccess("""
                        {
                          "id": 555002,
                          "status": "approved",
                          "status_detail": "accredited",
                          "external_reference": "other-order",
                          "transaction_amount": 28400.00
                        }
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://api.mercadopago.com/v1/payments/search?external_reference=ord-uuid-5"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer TEST-123456-fake-token"))
                .andRespond(withSuccess("""
                        {
                          "results": [
                            {
                              "id": 555002,
                              "status": "pending",
                              "external_reference": "ord-uuid-5",
                              "transaction_amount": 28400.00
                            },
                            {
                              "id": 555001,
                              "status": "approved",
                              "status_detail": "accredited",
                              "external_reference": "ord-uuid-5",
                              "transaction_amount": 28400.00
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        MercadoPagoService.PaymentVerificationResult ok = mercadoPagoService.verifyPaymentById(555001L, order);
        MercadoPagoService.PaymentVerificationResult mismatch = mercadoPagoService.verifyPaymentById(555002L, order);
        Optional<MercadoPagoService.PaymentVerificationResult> searched = mercadoPagoService.findApprovedPaymentForOrder(order);

        assertThat(ok.approved()).isTrue();
        assertThat(ok.mpPaymentId()).isEqualTo("555001");
        assertThat(mismatch.approved()).isFalse();
        assertThat(searched).isPresent();
        assertThat(searched.get().mpPaymentId()).isEqualTo("555001");
        mockServer.verify();
    }

    @Test
    @DisplayName("MP 400/401 error response propagates clear diagnostic message for official debugging")
    void createPreference_apiError_throwsClearDiagnostics() {
        Order order = sampleOrder("ord-uuid-err", 1010L, 14200.0, 0.0, 14200.0, "Lucas Mora");

        mockServer.expect(requestTo("https://api.mercadopago.com/checkout/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"invalid back_urls\",\"status\":400}"));

        assertThatThrownBy(() -> mercadoPagoService.createPreference(order, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("400")
                .hasMessageContaining("invalid back_urls");
        mockServer.verify();
    }

    private Order sampleOrder(String id, Long num, double subtotal, double discount, double total, String customer) {
        Product p = Product.builder()
                .id("prod-1")
                .code("ARC-01")
                .name("Bon o Bon Leche")
                .description("Display x 24 u.")
                .build();
        Order order = Order.builder()
                .id(id)
                .orderNumber(num)
                .orderDateTime(LocalDateTime.now())
                .status(OrderStatus.PENDING_PAYMENT)
                .paymentType(PaymentType.MERCADO_PAGO)
                .customerName(customer)
                .subtotal(subtotal)
                .discount(discount)
                .total(total)
                .build();
        OrderDetail d = OrderDetail.builder()
                .id("det-1")
                .product(p)
                .quantity(2)
                .unitPrice(14200.0)
                .subtotal(28400.0)
                .order(order)
                .build();
        order.getOrderDetails().add(d);
        return order;
    }
}
