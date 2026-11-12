package ing.software.dulcemayor.controller;

import ing.software.dulcemayor.dto.billing.MercadoPagoQrResponseDTO;
import ing.software.dulcemayor.dto.order.CreateOrderRequestDTO;
import ing.software.dulcemayor.dto.order.OrderResponseDTO;
import ing.software.dulcemayor.dto.order.UpdateOrderStatusDTO;
import ing.software.dulcemayor.dto.response.ApiResponse;
import ing.software.dulcemayor.service.billing.PaymentAccreditationService;
import ing.software.dulcemayor.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentAccreditationService paymentAccreditationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getAllOrders()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponseDTO>> createOrder(@RequestBody CreateOrderRequestDTO request) {
        OrderResponseDTO created = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Pedido registrado exitosamente con número #" + created.getOrderNumber(), created));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateStatus(
            @PathVariable String id,
            @RequestBody UpdateOrderStatusDTO request) {
        OrderResponseDTO updated = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Order status updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable String id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.ok("Pedido cancelado y eliminado correctamente", null));
    }

    @PostMapping("/{id}/mp/qr")
    public ResponseEntity<ApiResponse<MercadoPagoQrResponseDTO>> generateMercadoPagoQr(
            @PathVariable String id,
            @RequestParam(required = false) String returnBaseUrl) {
        MercadoPagoQrResponseDTO qr = paymentAccreditationService.generateOrFetchMercadoPagoQr(id, returnBaseUrl);
        return ResponseEntity.ok(ApiResponse.ok("QR de Mercado Pago generado", qr));
    }
}
