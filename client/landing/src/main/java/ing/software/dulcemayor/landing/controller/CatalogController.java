package ing.software.dulcemayor.landing.controller;

import ing.software.dulcemayor.landing.client.BackendApiClient;
import ing.software.dulcemayor.landing.dto.ProductDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CatalogController {

    private final BackendApiClient backendApiClient;

    @GetMapping("/catalog")
    public String catalog(
            @RequestParam(required = false) String search,
            Model model) {
        List<ProductDTO> products = backendApiClient.getCatalogProducts();

        if (search != null && !search.trim().isEmpty()) {
            String term = search.trim().toLowerCase();
            products = products.stream()
                    .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(term)) ||
                                 (p.getCode() != null && p.getCode().toLowerCase().contains(term)) ||
                                 (p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains(term)) ||
                                 (p.getSubCategoryName() != null && p.getSubCategoryName().toLowerCase().contains(term)) ||
                                 (p.getDescription() != null && p.getDescription().toLowerCase().contains(term)))
                    .toList();
        }

        model.addAttribute("title", "Catálogo Mayorista de Golosinas — Dulce Mayor");
        model.addAttribute("products", products);
        model.addAttribute("search", search != null ? search : "");
        return "catalog";
    }
}
