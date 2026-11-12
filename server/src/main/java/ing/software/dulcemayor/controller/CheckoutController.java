package ing.software.dulcemayor.controller;

import ing.software.dulcemayor.dto.billing.CheckoutRequestDTO;
import ing.software.dulcemayor.dto.billing.CheckoutResponseDTO;
import ing.software.dulcemayor.dto.billing.OrderPaymentStatusDTO;
import ing.software.dulcemayor.dto.order.OrderResponseDTO;
import ing.software.dulcemayor.dto.response.ApiResponse;
import ing.software.dulcemayor.service.billing.CheckoutService;
import ing.software.dulcemayor.service.billing.PaymentAccreditationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for storefront web checkout and customer order lookup.
 */
@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final PaymentAccreditationService paymentAccreditationService;

    @PostMapping
    public ResponseEntity<ApiResponse<CheckoutResponseDTO>> checkout(@RequestBody CheckoutRequestDTO request) {
        CheckoutResponseDTO response = checkoutService.processLandingCheckout(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response.getMessage(), response));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<OrderPaymentStatusDTO>> getCheckoutOrderStatus(@PathVariable String orderId) {
        OrderPaymentStatusDTO status = paymentAccreditationService.verifyOrPollOrderStatus(orderId, null);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    @GetMapping("/orders/search")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> searchCustomerOrders(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Long orderNumber) {
        List<OrderResponseDTO> orders = checkoutService.findCustomerOrders(email, orderNumber);
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }
}

