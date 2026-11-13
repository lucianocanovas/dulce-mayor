package ing.software.dulcemayor.service.catalog;

import ing.software.dulcemayor.dto.catalog.DollarQuoteDTO;
import ing.software.dulcemayor.dto.catalog.PriceValidityResponseDTO;
import ing.software.dulcemayor.entity.catalog.PriceValidity;
import ing.software.dulcemayor.entity.catalog.Product;
import ing.software.dulcemayor.repository.catalog.PriceValidityRepository;
import ing.software.dulcemayor.repository.catalog.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private final ProductRepository productRepository;
    private final PriceValidityRepository priceValidityRepository;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    private volatile DollarQuoteDTO cachedQuote = null;
    private volatile LocalDateTime lastFetchTime = null;
    private volatile String selectedRateMode = "OFICIAL"; // "OFICIAL" or "BLUE" or "CUSTOM"

    private static final double DEFAULT_REFERENCE_RATE = 1080.0;

    @Transactional
    public DollarQuoteDTO getCurrentQuoteAndEnsureSync(boolean forceRefresh) {
        boolean shouldRefresh = forceRefresh
                || cachedQuote == null
                || lastFetchTime == null
                || Duration.between(lastFetchTime, LocalDateTime.now()).toMinutes() >= 10;

        if (shouldRefresh) {
            DollarQuoteDTO fetched = fetchLiveDollarQuote();
            double targetRate = "BLUE".equalsIgnoreCase(selectedRateMode)
                    ? fetched.getBlueSellRate()
                    : fetched.getSellRate();
            String modeLabel = "BLUE".equalsIgnoreCase(selectedRateMode)
                    ? "Dólar Blue (Venta)"
                    : "Dólar Oficial (Venta)";

            fetched.setAppliedRate(targetRate);
            fetched.setRateType(modeLabel);

            int updated = synchronizeCatalogPrices(targetRate,
                    "Actualización por " + modeLabel + " (" + fetched.getSource() + ")");
            fetched.setUpdatedProductsCount(updated);

            cachedQuote = fetched;
            lastFetchTime = LocalDateTime.now();
        }

        return cachedQuote;
    }

    @Transactional
    public DollarQuoteDTO syncFromRawPayload(java.util.Map<String, Object> body) {
        String mode = "OFICIAL";
        Double customRate = null;
        if (body != null) {
            if (body.get("mode") != null) {
                mode = String.valueOf(body.get("mode")).trim().toUpperCase();
            }
            if (body.get("customRate") != null) {
                try {
                    double parsed = Double.parseDouble(String.valueOf(body.get("customRate")));
                    if (parsed >= 100 && parsed <= 50000) {
                        customRate = parsed;
                    } else {
                        throw new IllegalArgumentException("La cotización de referencia debe estar entre $100 y $50.000 ARS/USD.");
                    }
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("La cotización ingresada no tiene un formato numérico válido.");
                }
            }
        }
        return syncWithRateMode(mode, customRate);
    }

    @Transactional
    public DollarQuoteDTO syncWithRateMode(String mode, Double customRate) {
        DollarQuoteDTO baseQuote = fetchLiveDollarQuote();
        double targetRate;
        String modeLabel;

        if (customRate != null && customRate > 0) {
            selectedRateMode = "CUSTOM";
            targetRate = Math.round(customRate * 100.0) / 100.0;
            modeLabel = "Dólar Mayorista de Referencia";
        } else if ("BLUE".equalsIgnoreCase(mode)) {
            selectedRateMode = "BLUE";
            targetRate = baseQuote.getBlueSellRate();
            modeLabel = "Dólar Blue (Venta)";
        } else {
            selectedRateMode = "OFICIAL";
            targetRate = baseQuote.getSellRate();
            modeLabel = "Dólar Oficial (Venta)";
        }

        baseQuote.setAppliedRate(targetRate);
        baseQuote.setRateType(modeLabel);

        int updated = synchronizeCatalogPrices(
                targetRate,
                String.format(Locale.US, "Sincronización %s ($%.2f ARS/USD - %s)",
                        modeLabel, targetRate, baseQuote.getSource())
        );
        baseQuote.setUpdatedProductsCount(updated);

        cachedQuote = baseQuote;
        lastFetchTime = LocalDateTime.now();
        return cachedQuote;
    }

    @Transactional
    public int synchronizeCatalogPrices(double exchangeRate, String reasonLabel) {
        if (exchangeRate <= 0) {
            return 0;
        }

        List<Product> products = productRepository.findByIsDeletedFalse();
        int updatedCount = 0;

        for (Product product : products) {
            // Only auto-adjust products that have a basePriceUsd configured
            if (product.getBasePriceUsd() == null || product.getBasePriceUsd() <= 0) {
                continue;
            }

            double newPriceArs = Math.round((product.getBasePriceUsd() * exchangeRate) / 10.0) * 10.0;
            if (newPriceArs <= 0) {
                newPriceArs = Math.round(product.getBasePriceUsd() * exchangeRate);
            }

            var activePriceOpt = priceValidityRepository.findActivePriceByProductId(product.getId());
            if (activePriceOpt.isPresent()) {
                PriceValidity current = activePriceOpt.get();
                boolean priceChanged = Math.abs(current.getPrice() - newPriceArs) >= 1.0;
                boolean rateMissing = current.getExchangeRate() == null;

                if (priceChanged) {
                    current.setEndDate(LocalDate.now());
                    priceValidityRepository.save(current);

                    PriceValidity nextValidity = PriceValidity.builder()
                            .startDate(LocalDate.now())
                            .endDate(null)
                            .price(newPriceArs)
                            .usdPrice(product.getBasePriceUsd())
                            .exchangeRate(exchangeRate)
                            .reason(reasonLabel)
                            .product(product)
                            .build();
                    priceValidityRepository.save(nextValidity);
                    updatedCount++;
                } else if (rateMissing) {
                    current.setExchangeRate(exchangeRate);
                    current.setUsdPrice(product.getBasePriceUsd());
                    if (current.getReason() == null) {
                        current.setReason(reasonLabel);
                    }
                    priceValidityRepository.save(current);
                }
            } else {
                PriceValidity initialValidity = PriceValidity.builder()
                        .startDate(LocalDate.now())
                        .endDate(null)
                        .price(newPriceArs)
                        .usdPrice(product.getBasePriceUsd())
                        .exchangeRate(exchangeRate)
                        .reason(reasonLabel)
                        .product(product)
                        .build();
                priceValidityRepository.save(initialValidity);
                updatedCount++;
            }
        }

        return updatedCount;
    }

    @Transactional(readOnly = true)
    public List<PriceValidityResponseDTO> getPriceHistory(String productId) {
        List<PriceValidity> records = (productId != null && !productId.isBlank())
                ? priceValidityRepository.findByProductIdOrderByDateDesc(productId.trim())
                : priceValidityRepository.findAllWithProductOrderByProductAndDateDesc();

        double fallbackRate = getActiveExchangeRate();

        return records.stream().map(pv -> {
            Product p = pv.getProduct();
            String catName = (p.getSubCategory() != null && p.getSubCategory().getCategory() != null)
                    ? p.getSubCategory().getCategory().getName()
                    : "General";
            Double rate = pv.getExchangeRate() != null ? pv.getExchangeRate() : fallbackRate;
            Double usd = pv.getUsdPrice() != null
                    ? pv.getUsdPrice()
                    : (p.getBasePriceUsd() != null
                        ? p.getBasePriceUsd()
                        : Math.round((pv.getPrice() / rate) * 100.0) / 100.0);
            boolean active = pv.getEndDate() == null;

            return PriceValidityResponseDTO.builder()
                    .id(pv.getId())
                    .productId(p.getId())
                    .productCode(p.getCode())
                    .productName(p.getName())
                    .categoryName(catName)
                    .packaging(p.getPackaging())
                    .price(pv.getPrice())
                    .usdPrice(usd)
                    .exchangeRate(rate)
                    .startDate(pv.getStartDate())
                    .endDate(pv.getEndDate())
                    .currentlyValid(active)
                    .validityStatusLabel(active ? "Vigente en la actualidad" : "Finalizado")
                    .reason(pv.getReason() != null ? pv.getReason() : (active ? "Precio vigente actual" : "Vigencia histórica reemplazada"))
                    .build();
        }).toList();
    }

    public double getActiveExchangeRate() {
        if (cachedQuote != null && cachedQuote.getAppliedRate() > 0) {
            return cachedQuote.getAppliedRate();
        }
        return DEFAULT_REFERENCE_RATE;
    }

    private DollarQuoteDTO fetchLiveDollarQuote() {
        String nowFormatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        // 1. Primary API: DolarApi.com (DolarHoy data provider)
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://dolarapi.com/v1/dolares"))
                    .timeout(Duration.ofSeconds(4))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.isArray()) {
                    double oficialBuy = 0;
                    double oficialSell = 0;
                    double blueBuy = 0;
                    double blueSell = 0;

                    for (JsonNode node : root) {
                        String casa = node.path("casa").asString("");
                        if ("oficial".equalsIgnoreCase(casa)) {
                            oficialBuy = node.path("compra").asDouble(0);
                            oficialSell = node.path("venta").asDouble(0);
                        } else if ("blue".equalsIgnoreCase(casa)) {
                            blueBuy = node.path("compra").asDouble(0);
                            blueSell = node.path("venta").asDouble(0);
                        }
                    }

                    if (oficialSell > 0) {
                        if (blueSell <= 0) {
                            blueBuy = Math.round(oficialBuy * 1.15);
                            blueSell = Math.round(oficialSell * 1.15);
                        }
                        return DollarQuoteDTO.builder()
                                .buyRate(oficialBuy)
                                .sellRate(oficialSell)
                                .blueBuyRate(blueBuy)
                                .blueSellRate(blueSell)
                                .appliedRate(oficialSell)
                                .rateType("Dólar Oficial (Venta)")
                                .source("DolarApi / DolarHoy")
                                .updatedAt(nowFormatted)
                                .build();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("DolarApi primary fetch failed, trying fallback Bluelytics: {}", e.getMessage());
        }

        // 2. Secondary API: Bluelytics Argentina
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.bluelytics.com.ar/v2/latest"))
                    .timeout(Duration.ofSeconds(4))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
                JsonNode root = objectMapper.readTree(response.body());
                double oficialBuy = root.path("oficial").path("value_buy").asDouble(0);
                double oficialSell = root.path("oficial").path("value_sell").asDouble(0);
                double blueBuy = root.path("blue").path("value_buy").asDouble(0);
                double blueSell = root.path("blue").path("value_sell").asDouble(0);

                if (oficialSell > 0) {
                    return DollarQuoteDTO.builder()
                            .buyRate(oficialBuy)
                            .sellRate(oficialSell)
                            .blueBuyRate(blueBuy > 0 ? blueBuy : Math.round(oficialBuy * 1.15))
                            .blueSellRate(blueSell > 0 ? blueSell : Math.round(oficialSell * 1.15))
                            .appliedRate(oficialSell)
                            .rateType("Dólar Oficial (Venta)")
                            .source("Bluelytics / DolarHoy")
                            .updatedAt(nowFormatted)
                            .build();
                }
            }
        } catch (Exception e) {
            log.debug("Bluelytics fallback failed, using reference quote: {}", e.getMessage());
        }

        // 3. Offline fallback if no internet access
        return DollarQuoteDTO.builder()
                .buyRate(1075.0)
                .sellRate(1115.0)
                .blueBuyRate(1215.0)
                .blueSellRate(1235.0)
                .appliedRate(1115.0)
                .rateType("Dólar Oficial (Venta)")
                .source("Cotización Referencia DolarHoy")
                .updatedAt(nowFormatted)
                .build();
    }
}

