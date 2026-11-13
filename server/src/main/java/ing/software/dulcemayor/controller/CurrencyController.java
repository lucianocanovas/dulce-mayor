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
        DollarQuoteDTO synced = exchangeRateService.syncFromRawPayload(body);
        return ResponseEntity.ok(ApiResponse.ok("Precios sincronizados exitosamente con la cotización del dólar.", synced));
    }

    @GetMapping("/price-history")
    public ResponseEntity<ApiResponse<List<PriceValidityResponseDTO>>> getPriceHistory(
            @RequestParam(required = false) String productId) {
        List<PriceValidityResponseDTO> history = exchangeRateService.getPriceHistory(productId);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}
