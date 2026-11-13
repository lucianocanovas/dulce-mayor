package ing.software.dulcemayor.landing.client;

import ing.software.dulcemayor.landing.dto.AiChatRequestDTO;
import ing.software.dulcemayor.landing.dto.AiChatResponseDTO;
import ing.software.dulcemayor.landing.dto.ApiResponse;
import ing.software.dulcemayor.landing.dto.AuthResponseDTO;
import ing.software.dulcemayor.landing.dto.CheckoutRequestDTO;
import ing.software.dulcemayor.landing.dto.CheckoutResponseDTO;
import ing.software.dulcemayor.landing.dto.CustomerProfileDTO;
import ing.software.dulcemayor.landing.dto.CustomerRegisterRequestDTO;
import ing.software.dulcemayor.landing.dto.OrderPaymentStatusDTO;
import ing.software.dulcemayor.landing.dto.OrderSummaryDTO;
import ing.software.dulcemayor.landing.dto.PaymentConfigDTO;
import ing.software.dulcemayor.landing.dto.PointOfSaleDTO;
import ing.software.dulcemayor.landing.dto.ProductDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
                    .uri("/products/in-stock")
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<List<ProductDTO>>>() {});

            if (response != null && response.getData() != null) {
                return response.getData().stream()
                        .filter(p -> p.getCurrentStock() > 0)
                        .toList();
            }
        } catch (Exception e) {
            // Fallback to /products if needed
            try {
                ApiResponse<List<ProductDTO>> response = restClient.get()
                        .uri("/products")
                        .retrieve()
                        .body(new ParameterizedTypeReference<ApiResponse<List<ProductDTO>>>() {});
                if (response != null && response.getData() != null) {
                    return response.getData().stream()
                            .filter(p -> p.getCurrentStock() > 0)
                            .toList();
                }
            } catch (Exception ignored) {
                // Graceful fallback
            }
        }
        return Collections.emptyList();
    }

    public List<PointOfSaleDTO> getPointsOfSale() {
        try {
            ApiResponse<List<PointOfSaleDTO>> response = restClient.get()
                    .uri("/points-of-sale")
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<List<PointOfSaleDTO>>>() {});
            return response != null && response.getData() != null ? response.getData() : Collections.emptyList();
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    public PaymentConfigDTO getPaymentConfig() {
        try {
            ApiResponse<PaymentConfigDTO> response = restClient.get()
                    .uri("/payments/config")
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<PaymentConfigDTO>>() {});
            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception ignored) {
        }
        PaymentConfigDTO fallback = new PaymentConfigDTO();
        fallback.setSandboxEnabled(true);
        fallback.setBankAlias("DULCE.MAYOR.MZA");
        fallback.setBankCbu("0000003100084920193842");
        fallback.setBankHolder("Dulce Mayor S.A.");
        fallback.setBankName("Banco de la Nación Argentina");
        return fallback;
    }

    public CheckoutResponseDTO submitCheckout(CheckoutRequestDTO request) {
        try {
            ApiResponse<CheckoutResponseDTO> response = restClient.post()
                    .uri("/checkout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<CheckoutResponseDTO>>() {});
            if (response == null || response.getData() == null) {
                throw new IllegalStateException("El servidor no devolvió datos de la orden.");
            }
            return response.getData();
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(extractErrorMessage(ex));
        }
    }

    public OrderPaymentStatusDTO verifyMercadoPagoPayment(String orderId, String paymentId) {
        try {
            String uri = (paymentId != null && !paymentId.isBlank())
                    ? "/payments/mp/verify?orderId=" + orderId + "&payment_id=" + paymentId
                    : "/payments/mp/status/" + orderId;
            ApiResponse<OrderPaymentStatusDTO> response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<OrderPaymentStatusDTO>>() {});
            return response != null ? response.getData() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    public OrderPaymentStatusDTO getOrderStatus(String orderId) {
        try {
            ApiResponse<OrderPaymentStatusDTO> response = restClient.get()
                    .uri("/payments/mp/status/{orderId}", orderId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<OrderPaymentStatusDTO>>() {});
            return response != null ? response.getData() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    public List<OrderSummaryDTO> searchOrders(String email, Long orderNumber) {
        try {
            StringBuilder uri = new StringBuilder("/checkout/orders/search?");
            if (orderNumber != null) {
                uri.append("orderNumber=").append(orderNumber);
            } else if (email != null && !email.isBlank()) {
                uri.append("email=").append(email.trim());
            } else {
                return Collections.emptyList();
            }
            ApiResponse<List<OrderSummaryDTO>> response = restClient.get()
                    .uri(uri.toString())
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<List<OrderSummaryDTO>>>() {});
            return response != null && response.getData() != null ? response.getData() : Collections.emptyList();
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    public Map<String, Object> validatePromo(String code) {
        try {
            ApiResponse<Map<String, Object>> response = restClient.get()
                    .uri("/promotions/validate?code={code}", code.trim())
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {});
            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (RestClientResponseException ex) {
            throw new IllegalArgumentException(extractErrorMessage(ex));
        }
        throw new IllegalArgumentException("Cupón inválido o vencido.");
    }

    public List<OrderSummaryDTO> searchOrdersByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Collections.emptyList();
        }
        try {
            ApiResponse<List<OrderSummaryDTO>> response = restClient.get()
                    .uri("/checkout/orders/search?userId={userId}", userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<List<OrderSummaryDTO>>>() {});
            return response != null && response.getData() != null ? response.getData() : Collections.emptyList();
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    public AuthResponseDTO loginCustomer(String email, String password) {
        try {
            ApiResponse<AuthResponseDTO> response = restClient.post()
                    .uri("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("username", email.trim().toLowerCase(), "password", password))
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<AuthResponseDTO>>() {});
            if (response == null || response.getData() == null) {
                throw new IllegalArgumentException("Credenciales inválidas.");
            }
            return response.getData();
        } catch (RestClientResponseException ex) {
            throw new IllegalArgumentException(extractErrorMessage(ex));
        }
    }

    public AuthResponseDTO socialLogin(Map<String, Object> payload) {
        try {
            ApiResponse<AuthResponseDTO> response = restClient.post()
                    .uri("/auth/social")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<AuthResponseDTO>>() {});
            if (response == null || response.getData() == null) {
                throw new IllegalArgumentException("No se pudo iniciar sesión con Google.");
            }
            return response.getData();
        } catch (RestClientResponseException ex) {
            throw new IllegalArgumentException(extractErrorMessage(ex));
        }
    }

    public AuthResponseDTO registerCustomer(CustomerRegisterRequestDTO request) {
        try {
            ApiResponse<AuthResponseDTO> response = restClient.post()
                    .uri("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<AuthResponseDTO>>() {});
            if (response == null || response.getData() == null) {
                throw new IllegalArgumentException("No se pudo registrar la cuenta.");
            }
            return response.getData();
        } catch (RestClientResponseException ex) {
            throw new IllegalArgumentException(extractErrorMessage(ex));
        }
    }

    public CustomerProfileDTO getCustomerProfile(String userId) {
        try {
            ApiResponse<CustomerProfileDTO> response = restClient.get()
                    .uri("/auth/customer/{userId}", userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<CustomerProfileDTO>>() {});
            return response != null ? response.getData() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    public CustomerProfileDTO updateCustomerProfile(String userId, Map<String, Object> payload) {
        try {
            ApiResponse<CustomerProfileDTO> response = restClient.put()
                    .uri("/auth/customer/{userId}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<CustomerProfileDTO>>() {});
            if (response == null || response.getData() == null) {
                throw new IllegalArgumentException("No se pudo actualizar el perfil.");
            }
            return response.getData();
        } catch (RestClientResponseException ex) {
            throw new IllegalArgumentException(extractErrorMessage(ex));
        }
    }

    public void changePassword(String userId, String currentPassword, String newPassword) {
        try {
            restClient.post()
                    .uri("/auth/change-password?userId={userId}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("currentPassword", currentPassword, "newPassword", newPassword))
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<Void>>() {});
        } catch (RestClientResponseException ex) {
            throw new IllegalArgumentException(extractErrorMessage(ex));
        }
    }

    public List<PointOfSaleDTO> getCustomerStores(String userId) {
        if (userId == null || userId.isBlank()) {
            return Collections.emptyList();
        }
        try {
            ApiResponse<List<PointOfSaleDTO>> response = restClient.get()
                    .uri("/auth/customer/{userId}/stores", userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<List<PointOfSaleDTO>>>() {});
            return response != null && response.getData() != null ? response.getData() : Collections.emptyList();
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    public PointOfSaleDTO createCustomerStore(String userId, Map<String, Object> payload) {
        try {
            ApiResponse<PointOfSaleDTO> response = restClient.post()
                    .uri("/auth/customer/{userId}/stores", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<PointOfSaleDTO>>() {});
            if (response == null || response.getData() == null) {
                throw new IllegalArgumentException("No se pudo guardar el local.");
            }
            return response.getData();
        } catch (RestClientResponseException ex) {
            throw new IllegalArgumentException(extractErrorMessage(ex));
        }
    }

    public AiChatResponseDTO sendAiChat(AiChatRequestDTO request) {
        try {
            ApiResponse<AiChatResponseDTO> response = restClient.post()
                    .uri("/ai/quote")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<AiChatResponseDTO>>() {});
            if (response == null || response.getData() == null) {
                throw new IllegalStateException("El asistente no devolvió una respuesta.");
            }
            return response.getData();
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(extractErrorMessage(ex));
        }
    }

    private String extractErrorMessage(RestClientResponseException ex) {
        try {
            Map<?, ?> body = ex.getResponseBodyAs(Map.class);
            if (body != null && body.get("message") != null && !body.get("message").toString().isBlank()) {
                return body.get("message").toString();
            }
        } catch (Exception ignored) {
        }
        return "No se pudo completar la operación (" + ex.getStatusCode() + ").";
    }
}
