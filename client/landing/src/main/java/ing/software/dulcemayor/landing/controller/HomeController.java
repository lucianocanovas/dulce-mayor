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
        List<ProductDTO> featured = products.stream()
                .filter(ProductDTO::isOnSale)
                .limit(4)
                .toList();

        model.addAttribute("title", "Dulce Mayor — Distribuidora Mayorista");
        model.addAttribute("products", products);
        model.addAttribute("featuredProducts", featured);
        return "index";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("title", "Contacto & Sucursales — Dulce Mayor");
        return "contact";
    }
}
