package ing.software.dulcemayor.controller;

import ing.software.dulcemayor.dto.catalog.PromotionDTO;
import ing.software.dulcemayor.dto.response.ApiResponse;
import ing.software.dulcemayor.service.catalog.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PromotionDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(promotionService.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PromotionDTO>> create(@RequestBody PromotionDTO request) {
        PromotionDTO created = promotionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Promoción creada correctamente", created));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<PromotionDTO>> validateCode(@RequestParam String code) {
        PromotionDTO validPromo = promotionService.validateCode(code);
        return ResponseEntity.ok(ApiResponse.ok("Cupón válido", validPromo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        promotionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Promoción eliminada", null));
    }
}
