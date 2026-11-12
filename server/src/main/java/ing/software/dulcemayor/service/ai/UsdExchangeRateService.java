package ing.software.dulcemayor.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

/**
 * Fetches and caches the official USD/ARS sell rate from DolarApi for wholesale quote conversions.
 */
@Slf4j
@Service
public class UsdExchangeRateService {

    private static final double FALLBACK_USD_RATE = 1150.0;
    private static final long CACHE_TTL_SECONDS = 900; // 15 minutes

    private final RestClient restClient;
    private volatile double cachedRate = FALLBACK_USD_RATE;
    private volatile Instant lastFetched = Instant.EPOCH;

    public UsdExchangeRateService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://dolarapi.com/v1")
                .build();
    }

    public double getOfficialSellRate() {
        Instant now = Instant.now();
        if (now.isBefore(lastFetched.plusSeconds(CACHE_TTL_SECONDS))) {
            return cachedRate;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri("/dolares/oficial")
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.get("venta") instanceof Number venta) {
                double rate = venta.doubleValue();
                if (rate > 0) {
                    cachedRate = rate;
                    lastFetched = now;
                    return cachedRate;
                }
            }
        } catch (Exception ex) {
            log.debug("Could not fetch USD exchange rate from DolarApi, using cached/fallback rate: {}", ex.getMessage());
        }
        return cachedRate;
    }
}
