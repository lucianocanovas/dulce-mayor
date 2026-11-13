package ing.software.dulcemayor.landing.controller;

import ing.software.dulcemayor.landing.client.BackendApiClient;
import ing.software.dulcemayor.landing.dto.CheckoutRequestDTO;
import ing.software.dulcemayor.landing.dto.CheckoutResponseDTO;
import ing.software.dulcemayor.landing.dto.CustomerProfileDTO;
import ing.software.dulcemayor.landing.dto.OrderPaymentStatusDTO;
import ing.software.dulcemayor.landing.dto.OrderSummaryDTO;
import ing.software.dulcemayor.landing.dto.PaymentConfigDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final BackendApiClient backendApiClient;

    @GetMapping("/checkout")
    public String checkoutPage(HttpSession session, Model model) {
        if (!(session.getAttribute(GlobalModelAttributes.SESSION_CUSTOMER_KEY) instanceof CustomerProfileDTO customer)) {
            return "redirect:/login";
        }
        PaymentConfigDTO paymentConfig = backendApiClient.getPaymentConfig();
        model.addAttribute("title", "Finalizar Compra — Dulce Mayor");
        model.addAttribute("paymentConfig", paymentConfig);
        model.addAttribute("savedStores", backendApiClient.getCustomerStores(customer.getUserId()));
        return "checkout";
    }

    @PostMapping("/api/landing/checkout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitCheckout(
            @RequestBody CheckoutRequestDTO request,
            HttpServletRequest httpRequest,
            HttpSession session) {
        try {
            Object attr = session.getAttribute(GlobalModelAttributes.SESSION_CUSTOMER_KEY);
            if (!(attr instanceof CustomerProfileDTO customer)) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "requiresAuth", true,
                        "message", "Debés iniciar sesión con tu cuenta para realizar un pedido."
                ));
            }
            request.setUserId(customer.getUserId());
            if (request.getReturnBaseUrl() == null || request.getReturnBaseUrl().isBlank()) {
                String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName()
                        + (httpRequest.getServerPort() == 80 || httpRequest.getServerPort() == 443
                        ? "" : ":" + httpRequest.getServerPort());
                request.setReturnBaseUrl(baseUrl);
            }
            CheckoutResponseDTO response = backendApiClient.submitCheckout(request);
            return ResponseEntity.ok(Map.of("success", true, "data", response));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage() != null ? ex.getMessage() : "Error al procesar el pedido"
            ));
        }
    }

    @GetMapping("/checkout/success")
    public String checkoutSuccess(
            @RequestParam(required = false) String orderId,
            @RequestParam(name = "payment_id", required = false) String paymentId,
            @RequestParam(name = "collection_id", required = false) String collectionId,
            HttpSession session,
            Model model) {
        if (!(session.getAttribute(GlobalModelAttributes.SESSION_CUSTOMER_KEY) instanceof CustomerProfileDTO)) {
            return "redirect:/login";
        }
        String effectivePaymentId = (paymentId != null && !paymentId.isBlank()) ? paymentId : collectionId;
        OrderPaymentStatusDTO status = (orderId != null && !orderId.isBlank())
                ? backendApiClient.verifyMercadoPagoPayment(orderId, effectivePaymentId)
                : null;
        populateResultModel(model, orderId, status, "success");
        return "checkout-result";
    }

    @GetMapping("/checkout/pending")
    public String checkoutPending(
            @RequestParam(required = false) String orderId,
            HttpSession session,
            Model model) {
        if (!(session.getAttribute(GlobalModelAttributes.SESSION_CUSTOMER_KEY) instanceof CustomerProfileDTO)) {
            return "redirect:/login";
        }
        OrderPaymentStatusDTO status = (orderId != null && !orderId.isBlank())
                ? backendApiClient.getOrderStatus(orderId)
                : null;
        populateResultModel(model, orderId, status, "pending");
        return "checkout-result";
    }

    @GetMapping("/checkout/failure")
    public String checkoutFailure(
            @RequestParam(required = false) String orderId,
            HttpSession session,
            Model model) {
        if (!(session.getAttribute(GlobalModelAttributes.SESSION_CUSTOMER_KEY) instanceof CustomerProfileDTO)) {
            return "redirect:/login";
        }
        OrderPaymentStatusDTO status = (orderId != null && !orderId.isBlank())
                ? backendApiClient.getOrderStatus(orderId)
                : null;
        populateResultModel(model, orderId, status, "failure");
        return "checkout-result";
    }

    @GetMapping("/api/landing/orders/{orderId}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> pollOrderStatus(@PathVariable String orderId) {
        OrderPaymentStatusDTO status = backendApiClient.getOrderStatus(orderId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("success", true, "data", status));
    }

    @GetMapping("/api/landing/promotions/validate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validatePromo(@RequestParam String code) {
        try {
            Map<String, Object> promo = backendApiClient.validatePromo(code);
            return ResponseEntity.ok(Map.of("success", true, "data", promo));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage() != null ? ex.getMessage() : "Cupón inválido"
            ));
        }
    }

    @GetMapping("/my-orders")
    public String myOrders(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Long orderNumber,
            HttpSession session,
            Model model) {
        Object attr = session.getAttribute(GlobalModelAttributes.SESSION_CUSTOMER_KEY);
        if (!(attr instanceof CustomerProfileDTO customer)) {
            return "redirect:/login";
        }
        List<OrderSummaryDTO> orders;
        boolean hasExplicitFilter = (email != null && !email.isBlank()) || orderNumber != null;
        if (!hasExplicitFilter) {
            orders = backendApiClient.searchOrdersByUserId(customer.getUserId());
        } else {
            orders = backendApiClient.searchOrders(email, orderNumber);
        }
        model.addAttribute("title", "Mis Pedidos — Dulce Mayor");
        model.addAttribute("orders", orders);
        model.addAttribute("searchEmail", email != null ? email : "");
        model.addAttribute("searchOrderNumber", orderNumber);
        return "my-orders";
    }

    private void populateResultModel(
            Model model,
            String orderId,
            OrderPaymentStatusDTO status,
            String routeOutcome) {
        PaymentConfigDTO paymentConfig = backendApiClient.getPaymentConfig();
        model.addAttribute("title", "Estado de Compra — Dulce Mayor");
        model.addAttribute("orderId", orderId);
        model.addAttribute("orderStatus", status);
        model.addAttribute("routeOutcome", routeOutcome);
        model.addAttribute("paymentConfig", paymentConfig);
    }
}

