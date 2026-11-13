package ing.software.dulcemayor.service.ai;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ing.software.dulcemayor.dto.ai.AiChatMessageDTO;
import ing.software.dulcemayor.dto.ai.AiChatRequestDTO;
import ing.software.dulcemayor.dto.ai.AiChatResponseDTO;
import ing.software.dulcemayor.dto.ai.AiRecommendedItemDTO;
import ing.software.dulcemayor.dto.auth.CustomerProfileDTO;
import ing.software.dulcemayor.dto.catalog.ProductResponseDTO;
import ing.software.dulcemayor.dto.order.OrderDetailResponseDTO;
import ing.software.dulcemayor.dto.order.OrderResponseDTO;
import ing.software.dulcemayor.entity.catalog.Promotion;
import ing.software.dulcemayor.repository.catalog.PromotionRepository;
import ing.software.dulcemayor.service.actor.AuthService;
import ing.software.dulcemayor.service.billing.CheckoutService;
import ing.software.dulcemayor.service.catalog.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI Assistant service grounded with the live wholesale product catalog, active promotions,
 * USD exchange rate, and authenticated customer profile & order history.
 */
@Slf4j
@Service
public class AiQuoteService {

    private static final String DISCLAIMER = "Este presupuesto es informativo y está sujeto a confirmación final de stock y emisión de factura.";
    private static final Pattern BUDGET_PATTERN = Pattern.compile("(?:\\$|ars|usd|u\\$s|pesos|dolares|dólares)?\\s*(\\d[\\d.,]*)\\s*(mil|k)?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern QTY_BEFORE_WORD = Pattern.compile("(\\d+)\\s+(?:cajas?|bultos?|packs?|unidades?)?\\s*(?:de\\s+)?([a-zA-ZáéíóúñÁÉÍÓÚÑ0-9\\s]{3,25})");

    private final ProductService productService;
    private final PromotionRepository promotionRepository;
    private final AuthService authService;
    private final CheckoutService checkoutService;
    private final UsdExchangeRateService usdExchangeRateService;
    private final ObjectMapper objectMapper;
    private final RestClient geminiRestClient;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-3.5-flash-lite}")
    private String geminiModel;

    public AiQuoteService(
            ProductService productService,
            PromotionRepository promotionRepository,
            AuthService authService,
            CheckoutService checkoutService,
            UsdExchangeRateService usdExchangeRateService) {
        this.productService = productService;
        this.promotionRepository = promotionRepository;
        this.authService = authService;
        this.checkoutService = checkoutService;
        this.usdExchangeRateService = usdExchangeRateService;
        this.objectMapper = new ObjectMapper();
        this.geminiRestClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    @Transactional(readOnly = true)
    public AiChatResponseDTO chat(AiChatRequestDTO request) {
        String userMessage = (request != null && request.getMessage() != null)
                ? request.getMessage().trim()
                : "";
        if (userMessage.isEmpty()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío.");
        }

        List<ProductResponseDTO> catalog = productService.getInStockProducts().stream()
                .filter(p -> p.getCurrentStock() > 0)
                .toList();

        LocalDate today = LocalDate.now();
        List<Promotion> activePromos = promotionRepository.findByIsDeletedFalse().stream()
                .filter(p -> (p.getStartDate() == null || !today.isBefore(p.getStartDate()))
                        && (p.getEndDate() == null || !today.isAfter(p.getEndDate())))
                .toList();

        List<String> promoSummaries = activePromos.stream()
                .map(p -> p.getCode() + ": " + p.getDescription() + " (" + (int) p.getDiscountPercentage() + "% OFF)")
                .toList();

        double usdRate = usdExchangeRateService.getOfficialSellRate();

        CustomerProfileDTO customerProfile = null;
        List<OrderResponseDTO> customerOrders = List.of();
        if (request.getUserId() != null && !request.getUserId().isBlank()) {
            try {
                customerProfile = authService.getCustomerProfile(request.getUserId());
                customerOrders = checkoutService.findCustomerOrders(null, null, request.getUserId());
            } catch (Exception ex) {
                log.debug("Could not load customer context for userId {}: {}", request.getUserId(), ex.getMessage());
            }
        }

        boolean authenticated = customerProfile != null;

        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                AiChatResponseDTO geminiResponse = callGemini(
                        userMessage,
                        request.getHistory(),
                        catalog,
                        promoSummaries,
                        usdRate,
                        customerProfile,
                        customerOrders,
                        authenticated
                );
                if (geminiResponse != null) {
                    return geminiResponse;
                }
            } catch (Exception ex) {
                log.warn("Gemini API call failed, falling back to local catalog assistant: {}", ex.getMessage());
            }
        }

        return buildDeterministicFallbackResponse(
                userMessage,
                catalog,
                promoSummaries,
                usdRate,
                customerProfile,
                customerOrders,
                authenticated
        );
    }

    private AiChatResponseDTO callGemini(
            String userMessage,
            List<AiChatMessageDTO> history,
            List<ProductResponseDTO> catalog,
            List<String> promoSummaries,
            double usdRate,
            CustomerProfileDTO customerProfile,
            List<OrderResponseDTO> customerOrders,
            boolean authenticated) throws Exception {

        String systemPrompt = buildSystemPrompt(catalog, promoSummaries, usdRate, customerProfile, customerOrders);

        List<Map<String, Object>> contents = new ArrayList<>();
        if (history != null) {
            int start = Math.max(0, history.size() - 8);
            for (int i = start; i < history.size(); i++) {
                AiChatMessageDTO msg = history.get(i);
                if (msg.getContent() == null || msg.getContent().isBlank()) continue;
                String role = "assistant".equalsIgnoreCase(msg.getRole()) ? "model" : "user";
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", msg.getContent()))
                ));
            }
        }
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
        ));

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", contents,
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "responseMimeType", "application/json"
                )
        );

        String rawResponse = geminiRestClient.post()
                .uri("/v1beta/models/{model}:generateContent?key={key}", geminiModel, geminiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        if (rawResponse == null || rawResponse.isBlank()) {
            return null;
        }

        JsonNode root = objectMapper.readTree(rawResponse);
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            return null;
        }

        String jsonText = stripMarkdownCodeFence(textNode.asText());
        JsonNode parsed = objectMapper.readTree(jsonText);
        String reply = parsed.path("reply").asText("Te armé una propuesta con nuestro catálogo mayorista.");

        Map<String, ProductResponseDTO> catalogByCode = new LinkedHashMap<>();
        for (ProductResponseDTO p : catalog) {
            catalogByCode.put(p.getCode().toUpperCase(Locale.ROOT), p);
        }

        List<AiRecommendedItemDTO> recommendedItems = new ArrayList<>();
        double totalArs = 0.0;

        JsonNode itemsNode = parsed.path("items");
        if (itemsNode.isArray()) {
            for (JsonNode itemNode : itemsNode) {
                String code = itemNode.path("code").asText("").trim().toUpperCase(Locale.ROOT);
                int requestedQty = itemNode.path("quantity").asInt(0);
                ProductResponseDTO product = catalogByCode.get(code);
                if (product != null && requestedQty > 0 && product.getCurrentStock() > 0) {
                    int qty = Math.min(requestedQty, product.getCurrentStock());
                    double subtotal = qty * product.getCurrentPrice();
                    totalArs += subtotal;
                    recommendedItems.add(AiRecommendedItemDTO.builder()
                            .productId(product.getId())
                            .code(product.getCode())
                            .name(product.getName())
                            .packaging(product.getPackaging())
                            .unitsPerPack(product.getUnitsPerPack())
                            .quantity(qty)
                            .unitPrice(product.getCurrentPrice())
                            .subtotal(subtotal)
                            .currentStock(product.getCurrentStock())
                            .build());
                }
            }
        }

        Double totalUsd = totalArs > 0 ? Math.round((totalArs / usdRate) * 100.0) / 100.0 : null;

        return AiChatResponseDTO.builder()
                .reply(reply)
                .recommendedItems(recommendedItems)
                .estimatedTotal(totalArs)
                .estimatedTotalUsd(totalUsd)
                .usdRate(usdRate)
                .activePromotions(promoSummaries)
                .disclaimer(DISCLAIMER)
                .authenticated(authenticated)
                .aiPowered(true)
                .build();
    }

    private String buildSystemPrompt(
            List<ProductResponseDTO> catalog,
            List<String> promoSummaries,
            double usdRate,
            CustomerProfileDTO customerProfile,
            List<OrderResponseDTO> customerOrders) {

        StringBuilder sb = new StringBuilder();
        sb.append("Sos el asistente virtual mayorista de Dulce Mayor (Mendoza, Argentina, desde 1926). ");
        sb.append("Respondés siempre en español rioplatense cordial, claro y conciso.\n");
        sb.append("Reglas estrictas:\n");
        sb.append("1. NUNCA inventes productos, códigos, precios ni stock. Usá exclusivamente el catálogo vigente abajo.\n");
        sb.append("2. Si el cliente pide un presupuesto o surtido por cualquier monto (por ejemplo $50.000, 300mil, $300.000, etc.), calculá cantidades de productos reales del catálogo cuya suma se acerque al presupuesto solicitado sin superar el stock disponible de cada producto.\n");
        sb.append("3. Podés responder preguntas sobre sucursales, horarios, tiempos de entrega, medios de pago, promociones, cotización USD, estado de pedidos y también mantener una charla cordial breve (incluso un chiste corto relacionado si te lo piden, volviendo amablemente a ayudar con su compra).\n");
        sb.append("4. Cotización oficial de referencia: 1 USD = $").append(String.format(Locale.US, "%.2f", usdRate)).append(" ARS.\n");
        sb.append("5. Devolvé SIEMPRE un JSON válido con este formato exacto:\n");
        sb.append("{\"reply\": \"Texto conversacional para el cliente\", \"items\": [{\"code\": \"CODIGO\", \"quantity\": 2}]}\n");
        sb.append("Si la consulta no requiere armar un presupuesto de productos (ej: saludo, estado de pedido, sucursales, tiempos de entrega, charla), devolvé \"items\": [].\n\n");

        sb.append("INFORMACIÓN INSTITUCIONAL, SUCURSALES Y LOGÍSTICA:\n");
        sb.append("- Sede Central Mendoza: Av. San Martín 1500, Ciudad de Mendoza (Lunes a Viernes 08:00 a 18:00 hs | Sábados 08:30 a 12:30 hs).\n");
        sb.append("- Sucursal Godoy Cruz (Centro de Distribución Sur): Carril Rodríguez Peña 2140, Godoy Cruz (Lunes a Viernes 07:30 a 16:30 hs).\n");
        sb.append("- Depósito Guaymallén (Planta Logística Mayorista): Av. Acceso Este Km 1035, Guaymallén (Lunes a Sábados 07:00 a 15:00 hs).\n");
        sb.append("- Teléfono / WhatsApp comercial: +54 9 261 555-1234 | Correo: ventas@dulcemayor.com.ar.\n");
        sb.append("- Entregas y tiempos estimados: Cobertura en Gran Mendoza y alrededores. Demora estimada de 24 a 48 hs hábiles desde la confirmación del pedido (también disponible retiro en sucursal).\n");
        sb.append("- Medios de pago: Mercado Pago, Transferencia Bancaria (Alias: DULCE.MAYOR.MZA) y Efectivo Contra Entrega.\n\n");

        sb.append("CATÁLOGO VIGENTE EN STOCK:\n");
        for (ProductResponseDTO p : catalog) {
            sb.append(String.format(Locale.US,
                    "- [%s] %s | %s (%d u/pack) | Precio ARS: %.0f | Stock: %d%s\n",
                    p.getCode(), p.getName(),
                    p.getPackaging() != null ? p.getPackaging() : "Pack",
                    p.getUnitsPerPack(),
                    p.getCurrentPrice(),
                    p.getCurrentStock(),
                    p.isOnSale() ? " | EN OFERTA" : ""));
        }

        if (!promoSummaries.isEmpty()) {
            sb.append("\nCUPONES / PROMOCIONES VIGENTES:\n");
            for (String promo : promoSummaries) {
                sb.append("- ").append(promo).append("\n");
            }
        }

        if (customerProfile != null) {
            sb.append("\nINFORMACIÓN DEL CLIENTE AUTENTICADO (podés usarla para responderle):\n");
            sb.append("- Nombre: ").append(customerProfile.getFullName()).append("\n");
            sb.append("- Email: ").append(customerProfile.getEmail()).append("\n");
            if (customerProfile.getStoreName() != null) {
                sb.append("- Comercio: ").append(customerProfile.getStoreName()).append("\n");
            }
            if (customerProfile.getDeliveryAddress() != null) {
                sb.append("- Dirección registrada: ").append(customerProfile.getDeliveryAddress());
                if (customerProfile.getDeliveryNeighborhood() != null) {
                    sb.append(", ").append(customerProfile.getDeliveryNeighborhood());
                }
                if (customerProfile.getDeliveryCity() != null) {
                    sb.append(", ").append(customerProfile.getDeliveryCity());
                }
                sb.append("\n");
            }
            if (customerOrders != null && !customerOrders.isEmpty()) {
                sb.append("- Últimos pedidos del cliente:\n");
                int limit = Math.min(customerOrders.size(), 8);
                for (int i = 0; i < limit; i++) {
                    OrderResponseDTO o = customerOrders.get(i);
                    sb.append(String.format(Locale.US,
                            "  * Pedido #%d | Fecha: %s | Estado: %s | Total: $%.0f ARS | Pago: %s\n",
                            o.getOrderNumber(),
                            o.getOrderDateTime() != null ? o.getOrderDateTime().toLocalDate() : "-",
                            o.getStatus(),
                            o.getTotal(),
                            o.getPaymentType() != null ? o.getPaymentType() : "-"));
                    if (o.getItems() != null && !o.getItems().isEmpty()) {
                        sb.append("    Productos: ");
                        for (int j = 0; j < o.getItems().size(); j++) {
                            OrderDetailResponseDTO d = o.getItems().get(j);
                            if (j > 0) sb.append(", ");
                            sb.append(d.getQuantity()).append("x ").append(d.getProductName());
                        }
                        sb.append("\n");
                    }
                }
            } else {
                sb.append("- El cliente aún no registra pedidos.\n");
            }
        } else {
            sb.append("\nEl visitante NO inició sesión. Si pregunta por sus pedidos personales o datos de cuenta, invitalo a iniciar sesión o crear una cuenta en el portal.\n");
        }

        return sb.toString();
    }

    private AiChatResponseDTO buildDeterministicFallbackResponse(
            String userMessage,
            List<ProductResponseDTO> catalog,
            List<String> promoSummaries,
            double usdRate,
            CustomerProfileDTO customerProfile,
            List<OrderResponseDTO> customerOrders,
            boolean authenticated) {

        String norm = normalize(userMessage);

        // 1. Customer account / order history questions
        if (norm.contains("pedido") || norm.contains("orden") || norm.contains("compra")
                || norm.contains("estado") || norm.contains("cuenta") || norm.contains("direccion")
                || norm.contains("mis datos")) {
            if (!authenticated) {
                return AiChatResponseDTO.builder()
                        .reply("Para consultar el estado de tus pedidos o acceder a los datos de tu cuenta, por favor **iniciá sesión** o **creá una cuenta** desde el menú superior. ¡También podés buscar un pedido por email o número en **Mis Pedidos**!")
                        .recommendedItems(List.of())
                        .estimatedTotal(0)
                        .usdRate(usdRate)
                        .activePromotions(promoSummaries)
                        .disclaimer(DISCLAIMER)
                        .authenticated(false)
                        .aiPowered(false)
                        .build();
            }

            // Check if they asked to repeat their last order
            if ((norm.contains("repetir") || norm.contains("similar") || norm.contains("mismo") || norm.contains("armame"))
                    && customerOrders != null && !customerOrders.isEmpty()) {
                OrderResponseDTO last = customerOrders.get(0);
                List<AiRecommendedItemDTO> repeatItems = new ArrayList<>();
                double total = 0;
                if (last.getItems() != null) {
                    for (OrderDetailResponseDTO d : last.getItems()) {
                        catalog.stream()
                                .filter(p -> p.getId().equals(d.getProductId()) || p.getName().equalsIgnoreCase(d.getProductName()))
                                .findFirst()
                                .ifPresent(p -> {
                                    int qty = Math.min(d.getQuantity(), p.getCurrentStock());
                                    if (qty > 0) {
                                        repeatItems.add(AiRecommendedItemDTO.builder()
                                                .productId(p.getId())
                                                .code(p.getCode())
                                                .name(p.getName())
                                                .packaging(p.getPackaging())
                                                .unitsPerPack(p.getUnitsPerPack())
                                                .quantity(qty)
                                                .unitPrice(p.getCurrentPrice())
                                                .subtotal(qty * p.getCurrentPrice())
                                                .currentStock(p.getCurrentStock())
                                                .build());
                                    }
                                });
                    }
                    total = repeatItems.stream().mapToDouble(AiRecommendedItemDTO::getSubtotal).sum();
                }
                if (!repeatItems.isEmpty()) {
                    return AiChatResponseDTO.builder()
                            .reply(String.format(Locale.US,
                                    "¡Hola %s! Te preparé un presupuesto basado en tu último pedido (#%d) con precios y stock actualizados:",
                                    customerProfile.getFirstName(), last.getOrderNumber()))
                            .recommendedItems(repeatItems)
                            .estimatedTotal(total)
                            .estimatedTotalUsd(Math.round((total / usdRate) * 100.0) / 100.0)
                            .usdRate(usdRate)
                            .activePromotions(promoSummaries)
                            .disclaimer(DISCLAIMER)
                            .authenticated(true)
                            .aiPowered(false)
                            .build();
                }
            }

            StringBuilder info = new StringBuilder();
            info.append("¡Hola ").append(customerProfile.getFirstName()).append("! ");
            if (customerProfile.getStoreName() != null) {
                info.append("Tu comercio registrado es **").append(customerProfile.getStoreName()).append("**");
                if (customerProfile.getDeliveryAddress() != null) {
                    info.append(" (entrega en ").append(customerProfile.getDeliveryAddress());
                    if (customerProfile.getDeliveryCity() != null) {
                        info.append(", ").append(customerProfile.getDeliveryCity());
                    }
                    info.append(")");
                }
                info.append(".\n\n");
            }
            if (customerOrders == null || customerOrders.isEmpty()) {
                info.append("Todavía no tenés pedidos registrados en tu cuenta. ¿Querés que te arme un presupuesto mayorista?");
            } else {
                OrderResponseDTO latest = customerOrders.get(0);
                info.append(String.format(Locale.US,
                        "Tenés **%d pedido(s)** registrado(s). El más reciente es el **Pedido #%d** — Estado: **%s** — Total: **$%,.0f ARS**.",
                        customerOrders.size(), latest.getOrderNumber(), latest.getStatus(), latest.getTotal()));
            }

            return AiChatResponseDTO.builder()
                    .reply(info.toString())
                    .recommendedItems(List.of())
                    .estimatedTotal(0)
                    .usdRate(usdRate)
                    .activePromotions(promoSummaries)
                    .disclaimer(DISCLAIMER)
                    .authenticated(true)
                    .aiPowered(false)
                    .build();
        }

        // 2. Promo / USD rate / Offers query
        if (norm.contains("dolar") || norm.contains("usd") || norm.contains("cotizacion")) {
            return AiChatResponseDTO.builder()
                    .reply(String.format(Locale.US,
                            "La cotización oficial de referencia actual es **1 USD = $%.2f ARS**. Todos los presupuestos que armamos incluyen el equivalente estimado en dólares.",
                            usdRate))
                    .recommendedItems(List.of())
                    .estimatedTotal(0)
                    .usdRate(usdRate)
                    .activePromotions(promoSummaries)
                    .disclaimer(DISCLAIMER)
                    .authenticated(authenticated)
                    .aiPowered(false)
                    .build();
        }

        if (norm.contains("promo") || norm.contains("cupon") || norm.contains("descuento")) {
            String promoText = promoSummaries.isEmpty()
                    ? "Por el momento no hay cupones activos, pero podés aprovechar nuestras ofertas mayoristas destacadas en el catálogo."
                    : "Estos son los cupones vigentes que podés aplicar en el checkout:\n• " + String.join("\n• ", promoSummaries);
            return AiChatResponseDTO.builder()
                    .reply(promoText)
                    .recommendedItems(List.of())
                    .estimatedTotal(0)
                    .usdRate(usdRate)
                    .activePromotions(promoSummaries)
                    .disclaimer(DISCLAIMER)
                    .authenticated(authenticated)
                    .aiPowered(false)
                    .build();
        }

        if (norm.contains("sucursal") || norm.contains("donde quedan") || norm.contains("ubicacion")
                || norm.contains("horario") || norm.contains("telefono") || norm.contains("contacto")) {
            return AiChatResponseDTO.builder()
                    .reply("Contamos con **3 sedes en Mendoza**:\n"
                            + "• **Sede Central Mendoza:** Av. San Martín 1500, Ciudad (Lun a Vie 08:00 a 18:00 hs | Sáb 08:30 a 12:30 hs)\n"
                            + "• **Sucursal Godoy Cruz:** Carril Rodríguez Peña 2140 (Lun a Vie 07:30 a 16:30 hs)\n"
                            + "• **Depósito Guaymallén:** Av. Acceso Este Km 1035 (Lun a Sáb 07:00 a 15:00 hs)\n"
                            + "• **Teléfono:** +54 9 261 555-1234 | **Correo:** ventas@dulcemayor.com.ar")
                    .recommendedItems(List.of())
                    .estimatedTotal(0)
                    .usdRate(usdRate)
                    .activePromotions(promoSummaries)
                    .disclaimer(DISCLAIMER)
                    .authenticated(authenticated)
                    .aiPowered(false)
                    .build();
        }

        if (norm.contains("entrega") || norm.contains("envio") || norm.contains("demora")
                || norm.contains("tarda") || norm.contains("pago")) {
            return AiChatResponseDTO.builder()
                    .reply("Realizamos entregas en **Gran Mendoza y alrededores** con una demora estimada de **24 a 48 hs hábiles** una vez confirmado el pedido (también podés retirar por Sede Central).\n"
                            + "Aceptamos **Mercado Pago**, **Transferencia Bancaria** (`DULCE.MAYOR.MZA`) y **Efectivo Contra Entrega**.")
                    .recommendedItems(List.of())
                    .estimatedTotal(0)
                    .usdRate(usdRate)
                    .activePromotions(promoSummaries)
                    .disclaimer(DISCLAIMER)
                    .authenticated(authenticated)
                    .aiPowered(false)
                    .build();
        }

        // 3. Build a wholesale quote from catalog
        List<AiRecommendedItemDTO> items = buildQuoteItemsFromText(norm, catalog, usdRate);
        if (!items.isEmpty()) {
            double total = items.stream().mapToDouble(AiRecommendedItemDTO::getSubtotal).sum();
            double totalUsd = Math.round((total / usdRate) * 100.0) / 100.0;
            String greeting = authenticated ? "¡Hola " + customerProfile.getFirstName() + "! " : "¡Claro! ";
            return AiChatResponseDTO.builder()
                    .reply(greeting + "Armé este presupuesto mayorista con stock real disponible. Podés agregarlo directo al carrito o ajustar las cantidades:")
                    .recommendedItems(items)
                    .estimatedTotal(total)
                    .estimatedTotalUsd(totalUsd)
                    .usdRate(usdRate)
                    .activePromotions(promoSummaries)
                    .disclaimer(DISCLAIMER)
                    .authenticated(authenticated)
                    .aiPowered(false)
                    .build();
        }

        String welcome = authenticated
                ? "¡Hola " + customerProfile.getFirstName() + "! Soy el asistente de Dulce Mayor. Puedo armarte presupuestos por monto o producto (ej: *\"Surtido de alfajores y chocolates por $50.000\"*) o consultar el estado de tus pedidos."
                : "¡Hola! Soy el asistente de Dulce Mayor. Contame qué golosinas necesitás o tu presupuesto (ej: *\"Quiero armar un surtido de chocolates y gomitas por $50.000\"*) y te armo una cotización mayorista al instante.";

        return AiChatResponseDTO.builder()
                .reply(welcome)
                .recommendedItems(List.of())
                .estimatedTotal(0)
                .usdRate(usdRate)
                .activePromotions(promoSummaries)
                .disclaimer(DISCLAIMER)
                .authenticated(authenticated)
                .aiPowered(false)
                .build();
    }

    private List<AiRecommendedItemDTO> buildQuoteItemsFromText(String norm, List<ProductResponseDTO> catalog, double usdRate) {
        if (catalog.isEmpty()) return List.of();

        // Check for explicit budget in message
        double targetBudget = 0;
        Matcher budgetMatcher = BUDGET_PATTERN.matcher(norm);
        while (budgetMatcher.find()) {
            String rawNum = budgetMatcher.group(1).replace(".", "").replace(",", "");
            String unit = budgetMatcher.group(2);
            try {
                double val = Double.parseDouble(rawNum);
                if (unit != null && !unit.isBlank()) {
                    val *= 1000.0;
                }
                if (val >= 500) {
                    targetBudget = (norm.contains("usd") || norm.contains("dolar")) ? val * usdRate : val;
                    break;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // Match specific product keywords
        List<ProductResponseDTO> matched = new ArrayList<>();
        for (ProductResponseDTO p : catalog) {
            String pNorm = normalize(p.getName() + " " + (p.getDescription() != null ? p.getDescription() : ""));
            for (String token : norm.split("\\s+")) {
                if (token.length() >= 4 && pNorm.contains(token) && !matched.contains(p)) {
                    matched.add(p);
                }
            }
        }

        if (matched.isEmpty() && (norm.contains("oferta") || norm.contains("surtido") || norm.contains("presupuesto") || targetBudget > 0)) {
            matched = catalog.stream()
                    .sorted((a, b) -> Boolean.compare(b.isOnSale(), a.isOnSale()))
                    .limit(4)
                    .toList();
        }

        if (matched.isEmpty()) {
            return List.of();
        }

        // Check explicit quantities like "3 cajas de bon o bon"
        Map<String, Integer> explicitQtyByProductId = new LinkedHashMap<>();
        Matcher qtyMatcher = QTY_BEFORE_WORD.matcher(norm);
        while (qtyMatcher.find()) {
            int qty = Integer.parseInt(qtyMatcher.group(1));
            String phrase = qtyMatcher.group(2).trim();
            for (ProductResponseDTO p : matched) {
                if (normalize(p.getName()).contains(phrase) || phrase.contains(normalize(p.getName()).split("\\s+")[0])) {
                    explicitQtyByProductId.put(p.getId(), Math.min(qty, p.getCurrentStock()));
                }
            }
        }

        List<AiRecommendedItemDTO> result = new ArrayList<>();
        if (targetBudget > 0 && explicitQtyByProductId.isEmpty()) {
            double remaining = targetBudget;
            List<ProductResponseDTO> pool = matched.stream().limit(4).toList();
            Map<String, Integer> qtyMap = new LinkedHashMap<>();
            boolean added = true;
            while (added && remaining > 0) {
                added = false;
                for (ProductResponseDTO p : pool) {
                    int cur = qtyMap.getOrDefault(p.getId(), 0);
                    if (cur < p.getCurrentStock() && p.getCurrentPrice() <= remaining) {
                        qtyMap.put(p.getId(), cur + 1);
                        remaining -= p.getCurrentPrice();
                        added = true;
                    }
                }
            }
            if (qtyMap.isEmpty() && !pool.isEmpty()) {
                qtyMap.put(pool.get(0).getId(), 1);
            }
            for (ProductResponseDTO p : pool) {
                int qty = qtyMap.getOrDefault(p.getId(), 0);
                if (qty > 0) {
                    result.add(toRecommendedItem(p, qty));
                }
            }
        } else {
            for (ProductResponseDTO p : matched.stream().limit(4).toList()) {
                int qty = explicitQtyByProductId.getOrDefault(p.getId(), 1);
                qty = Math.max(1, Math.min(qty, p.getCurrentStock()));
                result.add(toRecommendedItem(p, qty));
            }
        }
        return result;
    }

    private AiRecommendedItemDTO toRecommendedItem(ProductResponseDTO p, int qty) {
        return AiRecommendedItemDTO.builder()
                .productId(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .packaging(p.getPackaging())
                .unitsPerPack(p.getUnitsPerPack())
                .quantity(qty)
                .unitPrice(p.getCurrentPrice())
                .subtotal(qty * p.getCurrentPrice())
                .currentStock(p.getCurrentStock())
                .build();
    }

    private String stripMarkdownCodeFence(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    private String normalize(String input) {
        if (input == null) return "";
        String n = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
