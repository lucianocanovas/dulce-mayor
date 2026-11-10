package ing.software.dulcemayor.controller;

import ing.software.dulcemayor.dto.order.CreatePointOfSaleRequestDTO;
import ing.software.dulcemayor.dto.order.PointOfSaleResponseDTO;
import ing.software.dulcemayor.dto.response.ApiResponse;
import ing.software.dulcemayor.service.order.PointOfSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/points-of-sale")
@RequiredArgsConstructor
public class PointOfSaleController {

    private final PointOfSaleService pointOfSaleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PointOfSaleResponseDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(pointOfSaleService.getAllPointsOfSale()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PointOfSaleResponseDTO>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(pointOfSaleService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PointOfSaleResponseDTO>> create(@RequestBody CreatePointOfSaleRequestDTO request) {
        PointOfSaleResponseDTO created = pointOfSaleService.createPointOfSale(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Punto de venta registrado con éxito", created));
    }
}
