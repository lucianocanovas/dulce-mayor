package ing.software.dulcemayor.controller;

import ing.software.dulcemayor.dto.catalog.DollarQuoteDTO;
import ing.software.dulcemayor.dto.catalog.PriceValidityResponseDTO;
import ing.software.dulcemayor.dto.response.ApiResponse;
import ing.software.dulcemayor.service.catalog.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/currency")
@RequiredArgsConstructor
public class CurrencyController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/dollar")
    public ResponseEntity<ApiResponse<DollarQuoteDTO>> getDollarQuote(
            @RequestParam(defaultValue = "false") boolean refresh) {
        DollarQuoteDTO quote = exchangeRateService.getCurrentQuoteAndEnsureSync(refresh);
        return ResponseEntity.ok(ApiResponse.ok(quote));
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<DollarQuoteDTO>> syncDollarQuote(
            @RequestBody(required = false) Map<String, Object> body) {
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
        DollarQuoteDTO synced = exchangeRateService.syncWithRateMode(mode, customRate);
        return ResponseEntity.ok(ApiResponse.ok("Precios sincronizados exitosamente con la cotización del dólar.", synced));
    }

    @GetMapping("/price-history")
    public ResponseEntity<ApiResponse<List<PriceValidityResponseDTO>>> getPriceHistory(
            @RequestParam(required = false) String productId) {
        List<PriceValidityResponseDTO> history = exchangeRateService.getPriceHistory(productId);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}
