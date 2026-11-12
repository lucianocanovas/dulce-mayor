package ing.software.dulcemayor.controller;

import ing.software.dulcemayor.controller.base.BaseController;
import ing.software.dulcemayor.dto.catalog.ProductRequestDTO;
import ing.software.dulcemayor.dto.catalog.ProductResponseDTO;
import ing.software.dulcemayor.dto.response.ApiResponse;
import ing.software.dulcemayor.entity.catalog.Product;
import ing.software.dulcemayor.service.catalog.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController extends BaseController<Product, ProductRequestDTO, ProductResponseDTO> {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        super(productService);
        this.productService = productService;
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>> getLowStock(
            @RequestParam(defaultValue = "20") int threshold) {
        List<ProductResponseDTO> lowStockProducts = productService.getLowStockProducts(threshold);
        return ResponseEntity.ok(ApiResponse.ok(lowStockProducts));
    }

    @GetMapping("/in-stock")
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>> getInStockProducts() {
        return ResponseEntity.ok(ApiResponse.ok(productService.getInStockProducts()));
    }
}
