package ing.software.dulcemayor.controller;

import ing.software.dulcemayor.controller.base.BaseController;
import ing.software.dulcemayor.dto.catalog.PromotionDTO;
import ing.software.dulcemayor.dto.response.ApiResponse;
import ing.software.dulcemayor.service.catalog.PromotionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController extends BaseController<PromotionDTO, PromotionDTO> {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        super(promotionService);
        this.promotionService = promotionService;
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<PromotionDTO>> validateCode(@RequestParam String code) {
        PromotionDTO validPromo = promotionService.validateCode(code);
        return ResponseEntity.ok(ApiResponse.ok("Cupón válido", validPromo));
    }

    @Override
    protected String getCreatedMessage() {
        return "Promoción creada correctamente";
    }

    @Override
    protected String getUpdatedMessage() {
        return "Promoción actualizada correctamente";
    }

    @Override
    protected String getDeletedMessage() {
        return "Promoción eliminada";
    }
}
