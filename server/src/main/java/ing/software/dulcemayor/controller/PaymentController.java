package ing.software.dulcemayor.controller;

import ing.software.dulcemayor.dto.billing.InvoiceResponseDTO;
import ing.software.dulcemayor.dto.billing.ManualPaymentConfirmDTO;
import ing.software.dulcemayor.dto.billing.MercadoPagoQrResponseDTO;
import ing.software.dulcemayor.dto.billing.OrderPaymentStatusDTO;
import ing.software.dulcemayor.dto.billing.PaymentConfigDTO;
import ing.software.dulcemayor.dto.response.ApiResponse;
import ing.software.dulcemayor.service.billing.PaymentAccreditationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST endpoints for Mercado Pago preferences/QR, webhooks, status verification,
 * manual bank transfer/cash confirmation, and invoices.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentAccreditationService paymentAccreditationService;

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<PaymentConfigDTO>> getPaymentConfig() {
        return ResponseEntity.ok(ApiResponse.ok(paymentAccreditationService.getPaymentConfig()));
    }

    @PostMapping("/orders/{orderId}/mp/qr")
    public ResponseEntity<ApiResponse<MercadoPagoQrResponseDTO>> createMercadoPagoQr(
            @PathVariable String orderId,
            @RequestParam(required = false) String returnBaseUrl) {
        MercadoPagoQrResponseDTO qr = paymentAccreditationService.generateOrFetchMercadoPagoQr(orderId, returnBaseUrl);
        return ResponseEntity.ok(ApiResponse.ok("QR de Mercado Pago generado", qr));
    }

    @GetMapping("/mp/status/{orderId}")
    public ResponseEntity<ApiResponse<OrderPaymentStatusDTO>> checkMercadoPagoStatus(
            @PathVariable String orderId,
            @RequestParam(required = false) String paymentId) {
        OrderPaymentStatusDTO status = paymentAccreditationService.verifyOrPollOrderStatus(orderId, paymentId);
        return ResponseEntity.ok(ApiResponse.ok(status.getMessage(), status));
    }

    @GetMapping("/mp/verify")
    public ResponseEntity<ApiResponse<OrderPaymentStatusDTO>> verifyMercadoPagoReturn(
            @RequestParam String orderId,
            @RequestParam(name = "payment_id", required = false) String paymentId,
            @RequestParam(name = "collection_id", required = false) String collectionId) {
        String effectivePaymentId = (paymentId != null && !paymentId.isBlank()) ? paymentId : collectionId;
        OrderPaymentStatusDTO status = paymentAccreditationService.verifyOrPollOrderStatus(orderId, effectivePaymentId);
        return ResponseEntity.ok(ApiResponse.ok(status.getMessage(), status));
    }

    @GetMapping("/mp/webhook")
    public ResponseEntity<Map<String, Object>> handleMercadoPagoIpnGet(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String id,
            @RequestParam(name = "data.id", required = false) String dataId) {
        String effectiveTopic = (type != null && !type.isBlank()) ? type : topic;
        paymentAccreditationService.handleMercadoPagoWebhook(effectiveTopic, id, dataId, null);
        return ResponseEntity.ok(Map.of("received", true));
    }

    @PostMapping("/mp/webhook")
    public ResponseEntity<Map<String, Object>> handleMercadoPagoWebhookPost(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String id,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestBody(required = false) Map<String, Object> body) {
        String effectiveTopic = (type != null && !type.isBlank()) ? type : topic;
        paymentAccreditationService.handleMercadoPagoWebhook(effectiveTopic, id, dataId, body);
        return ResponseEntity.ok(Map.of("received", true));
    }

    @PostMapping("/orders/{orderId}/confirm-manual")
    public ResponseEntity<ApiResponse<OrderPaymentStatusDTO>> confirmManualPayment(
            @PathVariable String orderId,
            @RequestBody(required = false) ManualPaymentConfirmDTO request) {
        OrderPaymentStatusDTO status = paymentAccreditationService.confirmManualPayment(orderId, request);
        return ResponseEntity.ok(ApiResponse.ok("Pago confirmado y acreditado correctamente", status));
    }

    @GetMapping("/orders/{orderId}/invoice")
    public ResponseEntity<ApiResponse<InvoiceResponseDTO>> getInvoiceForOrder(@PathVariable String orderId) {
        InvoiceResponseDTO invoice = paymentAccreditationService.getInvoiceForOrder(orderId);
        return ResponseEntity.ok(ApiResponse.ok(invoice));
    }
}

