package ing.software.dulcemayor.landing.controller;

import ing.software.dulcemayor.landing.client.BackendApiClient;
import ing.software.dulcemayor.landing.dto.ProductDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final BackendApiClient backendApiClient;

    @GetMapping("/")
    public String index(Model model) {
        List<ProductDTO> products = backendApiClient.getCatalogProducts();
        List<ProductDTO> onSale = products.stream()
                .filter(ProductDTO::isOnSale)
                .limit(10)
                .toList();

        List<ProductDTO> featured;
        if (onSale.size() >= 4) {
            featured = onSale;
        } else {
            List<ProductDTO> combined = new java.util.ArrayList<>(onSale);
            for (ProductDTO p : products) {
                if (combined.size() >= 8) break;
                if (!p.isOnSale()) {
                    combined.add(p);
                }
            }
            featured = combined;
        }

        model.addAttribute("title", "Dulce Mayor — Distribuidora Mayorista");
        model.addAttribute("products", products);
        model.addAttribute("featuredProducts", featured);
        return "index";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("title", "Contacto & Sucursales — Dulce Mayor");
        model.addAttribute("pointsOfSale", backendApiClient.getPointsOfSale());
        return "contact";
    }
}
