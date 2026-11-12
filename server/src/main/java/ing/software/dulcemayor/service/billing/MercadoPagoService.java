package ing.software.dulcemayor.service.billing;

import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.entity.order.OrderDetail;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Integrates directly with Mercado Pago REST API v1 via Spring RestClient:
 * - POST https://api.mercadopago.com/checkout/preferences
 * - GET  https://api.mercadopago.com/v1/payments/{paymentId}
 * - GET  https://api.mercadopago.com/v1/payments/search?external_reference={orderId}
 *
 * Rules aligned with zero-shop:
 * - Always read init_point (never sandbox_init_point).
 * - Omit payer.email to prevent Sandbox Checkout Pro blocks.
 * - Only set auto_return and notification_url on public HTTPS URLs (no http, localhost, or 127.0.0.1).
 * - Always link external_reference to order.getId() in ARS currency.
 */
@Service
@Slf4j
public class MercadoPagoService {

    public static final String MP_API_BASE_URL = "https://api.mercadopago.com";
    private static final double AMOUNT_TOLERANCE_ARS = 0.10;

    private final RestClient restClient;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Getter
    @Value("${mercadopago.public-key:}")
    private String publicKey;

    @Getter
    @Value("${mercadopago.sandbox.enabled:true}")
    private boolean sandboxEnabled;

    @Value("${app.base-url:http://localhost:8080}")
    private String backendBaseUrl;

    @Value("${app.frontend-url:http://localhost:8081}")
    private String frontendUrl;

    @Autowired
    public MercadoPagoService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(MP_API_BASE_URL)
                .build();
    }

    protected MercadoPagoService() {
        this(RestClient.builder());
    }

    @PostConstruct
    public void init() {
        log.info("Mercado Pago REST client initialized (sandboxEnabled={}, tokenConfigured={})",
                sandboxEnabled, isConfigured());
    }

    public boolean isConfigured() {
        return accessToken != null && !accessToken.isBlank();
    }

    public PreferenceResult createPreference(Order order, String overrideFrontendUrl) {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "MERCADOPAGO_ACCESS_TOKEN no está configurado en server/.env o variables de entorno.");
        }
        Map<String, Object> payload = buildPreferencePayload(order, overrideFrontendUrl);
        try {
            PreferenceResult preference = executeCreatePreference(payload);
            if (preference == null || preference.initPoint() == null || preference.initPoint().isBlank()) {
                throw new IllegalStateException("Mercado Pago devolvió una preferencia sin init_point.");
            }
            return preference;
        } catch (RestClientResponseException ex) {
            String apiDetails = ex.getResponseBodyAsString();
            log.error("Mercado Pago API error creating preference for order {} (status={}): {}",
                    order.getId(), ex.getStatusCode(), apiDetails);
            throw new IllegalStateException(
                    "Error de Mercado Pago (" + ex.getStatusCode().value() + ") al crear la preferencia: " + apiDetails,
                    ex);
        } catch (RestClientException ex) {
            log.error("Mercado Pago HTTP error creating preference for order {}: {}",
                    order.getId(), ex.getMessage());
            throw new IllegalStateException("No se pudo conectar con la API de Mercado Pago: " + ex.getMessage(), ex);
        }
    }

    public Map<String, Object> buildPreferencePayload(Order order, String overrideFrontendUrl) {
        String effectiveFrontend = sanitizeBaseUrl(
                overrideFrontendUrl != null && !overrideFrontendUrl.isBlank() ? overrideFrontendUrl : frontendUrl,
                "http://localhost:8081"
        );
        String successUrl = effectiveFrontend + "/checkout/success?orderId=" + order.getId();
        String pendingUrl = effectiveFrontend + "/checkout/pending?orderId=" + order.getId();
        String failureUrl = effectiveFrontend + "/checkout/failure?orderId=" + order.getId();

        Map<String, Object> backUrls = new LinkedHashMap<>();
        backUrls.put("success", successUrl);
        backUrls.put("pending", pendingUrl);
        backUrls.put("failure", failureUrl);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", buildPreferenceItems(order));
        payload.put("payer", buildPayerWithoutEmail(order));
        payload.put("back_urls", backUrls);
        payload.put("external_reference", order.getId());
        payload.put("statement_descriptor", "DULCE MAYOR");

        String sanitizedBackend = sanitizeBaseUrl(backendBaseUrl, "http://localhost:8080");
        if (isPublicHttpsUrl(sanitizedBackend)) {
            payload.put("notification_url", sanitizedBackend + "/api/payments/mp/webhook");
            if (isPublicHttpsUrl(successUrl)) {
                payload.put("auto_return", "approved");
            }
        }

        return payload;
    }

    public PaymentVerificationResult verifyPaymentById(Long paymentId, Order expectedOrder) {
        if (!isConfigured()) {
            return PaymentVerificationResult.unapproved("MERCADOPAGO_ACCESS_TOKEN no configurado");
        }
        try {
            Optional<MpPaymentInfo> paymentOpt = executeGetPayment(paymentId);
            return paymentOpt
                    .map(payment -> evaluatePayment(payment, expectedOrder))
                    .orElseGet(() -> PaymentVerificationResult.unapproved("Pago inexistente en Mercado Pago"));
        } catch (RestClientResponseException ex) {
            String apiDetails = ex.getResponseBodyAsString();
            log.warn("MP API error checking payment {} (status={}): {}", paymentId, ex.getStatusCode(), apiDetails);
            return PaymentVerificationResult.unapproved("Error consultando pago en Mercado Pago: " + apiDetails);
        } catch (RestClientException ex) {
            log.warn("MP HTTP error checking payment {}: {}", paymentId, ex.getMessage());
            return PaymentVerificationResult.unapproved(
                    "No se pudo verificar el pago con Mercado Pago: " + ex.getMessage());
        }
    }

    public Optional<PaymentVerificationResult> findApprovedPaymentForOrder(Order order) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            List<MpPaymentInfo> results = executeSearchPayments(order.getId());
            return results.stream()
                    .sorted(Comparator.comparing(MpPaymentInfo::id, Comparator.nullsLast(Comparator.reverseOrder())))
                    .map(payment -> evaluatePayment(payment, order))
                    .filter(PaymentVerificationResult::approved)
                    .findFirst();
        } catch (RestClientResponseException ex) {
            log.warn("MP API error searching payments for order {} (status={}): {}",
                    order.getId(), ex.getStatusCode(), ex.getResponseBodyAsString());
            return Optional.empty();
        } catch (RestClientException ex) {
            log.warn("MP HTTP error searching payments for order {}: {}", order.getId(), ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<MpPaymentInfo> fetchRawPaymentById(Long paymentId) {
        if (!isConfigured() || paymentId == null) {
            return Optional.empty();
        }
        try {
            return executeGetPayment(paymentId);
        } catch (Exception ex) {
            log.warn("Could not fetch MP payment {}: {}", paymentId, ex.getMessage());
            return Optional.empty();
        }
    }

    public PaymentVerificationResult evaluatePayment(MpPaymentInfo payment, Order expectedOrder) {
        if (payment == null) {
            return PaymentVerificationResult.unapproved("Pago inexistente en Mercado Pago");
        }
        String status = payment.status();
        String extRef = payment.externalReference();
        BigDecimal txAmount = payment.transactionAmount();
        String mpPaymentId = payment.id() != null ? payment.id().toString() : null;

        if (!"approved".equalsIgnoreCase(status)) {
            return new PaymentVerificationResult(
                    false,
                    mpPaymentId,
                    status,
                    payment.statusDetail(),
                    extRef,
                    txAmount,
                    "Pago aún no aprobado (estado: " + status + ")"
            );
        }

        if (extRef != null && !extRef.isBlank() && !extRef.equals(expectedOrder.getId())) {
            return new PaymentVerificationResult(
                    false,
                    mpPaymentId,
                    "rejected_mismatch",
                    "external_reference_mismatch",
                    extRef,
                    txAmount,
                    "El external_reference no coincide con el pedido"
            );
        }

        if (txAmount != null) {
            double diff = Math.abs(txAmount.doubleValue() - expectedOrder.getTotal());
            if (diff > AMOUNT_TOLERANCE_ARS) {
                return new PaymentVerificationResult(
                        false,
                        mpPaymentId,
                        "rejected_amount_mismatch",
                        "amount_mismatch",
                        extRef,
                        txAmount,
                        "El monto abonado no coincide con el total del pedido"
                );
            }
        }

        return new PaymentVerificationResult(
                true,
                mpPaymentId,
                status,
                payment.statusDetail(),
                extRef != null ? extRef : expectedOrder.getId(),
                txAmount,
                "Pago aprobado y verificado"
        );
    }

    public boolean isPublicHttpsUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lower = url.trim().toLowerCase();
        return lower.startsWith("https://")
                && !lower.contains("localhost")
                && !lower.contains("127.0.0.1");
    }

    protected PreferenceResult executeCreatePreference(Map<String, Object> payload) {
        String rawJson = restClient.post()
                .uri("/checkout/preferences")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.trim())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(payload))
                .retrieve()
                .body(String.class);

        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }
        JsonNode root = jsonMapper.readTree(rawJson);
        String id = root.path("id").asString("");
        String initPoint = root.path("init_point").asString("");
        return new PreferenceResult(id.isBlank() ? null : id, initPoint.isBlank() ? null : initPoint);
    }

    protected Optional<MpPaymentInfo> executeGetPayment(Long paymentId) {
        String rawJson = restClient.get()
                .uri("/v1/payments/{paymentId}", paymentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.trim())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);

        if (rawJson == null || rawJson.isBlank()) {
            return Optional.empty();
        }
        JsonNode node = jsonMapper.readTree(rawJson);
        return Optional.of(parsePaymentNode(node));
    }

    protected List<MpPaymentInfo> executeSearchPayments(String orderId) {
        String rawJson = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/payments/search")
                        .queryParam("external_reference", orderId)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.trim())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);

        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        JsonNode resultsNode = jsonMapper.readTree(rawJson).path("results");
        if (!resultsNode.isArray()) {
            return List.of();
        }
        List<MpPaymentInfo> payments = new ArrayList<>();
        for (JsonNode item : resultsNode) {
            payments.add(parsePaymentNode(item));
        }
        return payments;
    }

    private MpPaymentInfo parsePaymentNode(JsonNode node) {
        Long id = node.hasNonNull("id") ? node.path("id").asLong() : null;
        String status = node.hasNonNull("status") ? node.path("status").asString(null) : null;
        String statusDetail = node.hasNonNull("status_detail") ? node.path("status_detail").asString(null) : null;
        String externalRef = node.hasNonNull("external_reference")
                ? node.path("external_reference").asString(null)
                : null;
        BigDecimal txAmount = node.hasNonNull("transaction_amount")
                ? node.path("transaction_amount").decimalValue()
                : null;
        return new MpPaymentInfo(id, status, statusDetail, externalRef, txAmount);
    }

    private List<Map<String, Object>> buildPreferenceItems(Order order) {
        List<OrderDetail> details = order.getOrderDetails() != null ? order.getOrderDetails() : List.of();
        if (details.isEmpty()) {
            return List.of(createItemMap(
                    order.getId(),
                    "Pedido Dulce Mayor #" + order.getOrderNumber(),
                    "Compra mayorista Dulce Mayor",
                    1,
                    BigDecimal.valueOf(Math.max(order.getTotal(), 0.01)).setScale(2, RoundingMode.HALF_UP)
            ));
        }

        if (order.getDiscount() > 0 && order.getSubtotal() > 0) {
            double factor = order.getTotal() / order.getSubtotal();
            List<Map<String, Object>> scaledItems = new ArrayList<>();
            BigDecimal accumulated = BigDecimal.ZERO;
            for (OrderDetail detail : details) {
                BigDecimal unitPrice = BigDecimal.valueOf(detail.getUnitPrice() * factor)
                        .setScale(2, RoundingMode.HALF_UP);
                accumulated = accumulated.add(unitPrice.multiply(BigDecimal.valueOf(detail.getQuantity())));
                scaledItems.add(createItemMap(
                        detail.getProduct().getId(),
                        detail.getProduct().getName(),
                        resolveItemDescription(detail),
                        detail.getQuantity(),
                        unitPrice
                ));
            }
            BigDecimal expected = BigDecimal.valueOf(order.getTotal()).setScale(2, RoundingMode.HALF_UP);
            if (accumulated.subtract(expected).abs().compareTo(BigDecimal.valueOf(0.05)) <= 0) {
                return scaledItems;
            }
            return List.of(createItemMap(
                    order.getId(),
                    "Pedido Dulce Mayor #" + order.getOrderNumber() + " (descuento incluido)",
                    "Compra mayorista con cupón de descuento aplicado",
                    1,
                    expected
            ));
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderDetail detail : details) {
            items.add(createItemMap(
                    detail.getProduct().getId(),
                    detail.getProduct().getName(),
                    resolveItemDescription(detail),
                    detail.getQuantity(),
                    BigDecimal.valueOf(detail.getUnitPrice()).setScale(2, RoundingMode.HALF_UP)
            ));
        }
        return items;
    }

    private Map<String, Object> createItemMap(
            String id,
            String title,
            String description,
            int quantity,
            BigDecimal unitPrice
    ) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("title", title);
        item.put("description", description);
        item.put("quantity", quantity);
        item.put("unit_price", unitPrice);
        item.put("currency_id", "ARS");
        return item;
    }

    private String resolveItemDescription(OrderDetail detail) {
        if (detail.getProduct() == null) {
            return "Producto mayorista Dulce Mayor";
        }
        String desc = detail.getProduct().getDescription();
        if (desc != null && !desc.isBlank()) {
            return desc.trim();
        }
        String packaging = detail.getProduct().getPackaging();
        if (packaging != null && !packaging.isBlank()) {
            return detail.getProduct().getName() + " (" + packaging.trim() + ")";
        }
        return detail.getProduct().getName();
    }

    private Map<String, Object> buildPayerWithoutEmail(Order order) {
        String rawName = order.getCustomerName();
        if ((rawName == null || rawName.isBlank()) && order.getPointOfSale() != null
                && order.getPointOfSale().getRetailer() != null) {
            rawName = (order.getPointOfSale().getRetailer().getFirstName() + " "
                    + order.getPointOfSale().getRetailer().getLastName()).trim();
        }
        if (rawName == null || rawName.isBlank()) {
            rawName = "Cliente Mayorista";
        }
        String[] parts = rawName.trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "Dulce Mayor";

        // Intentionally omit email so Sandbox Checkout Pro never rejects test buyer accounts
        Map<String, Object> payer = new LinkedHashMap<>();
        payer.put("name", firstName);
        payer.put("surname", lastName);
        return payer;
    }

    private String sanitizeBaseUrl(String candidate, String fallback) {
        String value = (candidate == null || candidate.isBlank()) ? fallback : candidate.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public record PreferenceResult(String preferenceId, String initPoint) {}

    public record MpPaymentInfo(
            Long id,
            String status,
            String statusDetail,
            String externalReference,
            BigDecimal transactionAmount
    ) {}

    public record PaymentVerificationResult(
            boolean approved,
            String mpPaymentId,
            String status,
            String statusDetail,
            String externalReference,
            BigDecimal transactionAmount,
            String message
    ) {
        public static PaymentVerificationResult unapproved(String message) {
            return new PaymentVerificationResult(false, null, "unapproved", null, null, null, message);
        }
    }
}
