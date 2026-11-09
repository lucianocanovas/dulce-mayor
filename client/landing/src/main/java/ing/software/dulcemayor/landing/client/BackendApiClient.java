package ing.software.dulcemayor.landing.client;

import ing.software.dulcemayor.landing.dto.ApiResponse;
import ing.software.dulcemayor.landing.dto.ProductDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Component
public class BackendApiClient {

    private final RestClient restClient;

    public BackendApiClient(@Value("${backend.api.url}") String backendApiUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(backendApiUrl)
                .build();
    }

    public List<ProductDTO> getCatalogProducts() {
        try {
            ApiResponse<List<ProductDTO>> response = restClient.get()
                    .uri("/products")
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<List<ProductDTO>>>() {});

            if (response != null && response.getData() != null) {
                // Business Rule 2: Exclude products with currentStock <= 0 from public storefront
                return response.getData().stream()
                        .filter(p -> p.getCurrentStock() > 0)
                        .toList();
            }
        } catch (Exception e) {
            // Graceful fallback if backend server is not running or unreachable
        }
        return Collections.emptyList();
    }
}
