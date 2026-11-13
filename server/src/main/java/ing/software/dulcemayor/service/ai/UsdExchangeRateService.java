package ing.software.dulcemayor.service.ai;

import ing.software.dulcemayor.dto.catalog.DollarQuoteDTO;
import ing.software.dulcemayor.service.catalog.ExchangeRateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Provides the unified USD/ARS exchange rate for AI wholesale quote conversions
 * by delegating to the single singleton source of truth {@link ExchangeRateService}.
 */
@Slf4j
@Service
public class UsdExchangeRateService {

    private static final double FALLBACK_USD_RATE = 1115.0;

    private final ExchangeRateService exchangeRateService;

    public UsdExchangeRateService() {
        this.exchangeRateService = null;
    }

    @Autowired
    public UsdExchangeRateService(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    public double getOfficialSellRate() {
        if (exchangeRateService != null) {
            try {
                DollarQuoteDTO quote = exchangeRateService.getCurrentQuoteAndEnsureSync(false);
                if (quote != null && quote.getAppliedRate() > 0) {
                    return quote.getAppliedRate();
                }
                double active = exchangeRateService.getActiveExchangeRate();
                if (active > 0) {
                    return active;
                }
            } catch (Exception ex) {
                log.debug("Could not resolve USD rate from ExchangeRateService, using fallback: {}", ex.getMessage());
            }
        }
        return FALLBACK_USD_RATE;
    }
}
