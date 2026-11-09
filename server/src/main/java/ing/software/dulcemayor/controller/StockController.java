package ing.software.dulcemayor.controller;

import ing.software.dulcemayor.dto.catalog.ProductResponseDTO;
import ing.software.dulcemayor.dto.catalog.StockAdjustmentDTO;
import ing.software.dulcemayor.dto.response.ApiResponse;
import ing.software.dulcemayor.service.catalog.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final ProductService productService;

    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> adjustStock(@RequestBody StockAdjustmentDTO request) {
        ProductResponseDTO updated = productService.adjustStock(request);
        return ResponseEntity.ok(ApiResponse.ok("Stock updated successfully", updated));
    }
}
