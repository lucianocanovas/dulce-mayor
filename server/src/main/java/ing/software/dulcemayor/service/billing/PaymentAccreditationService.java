package ing.software.dulcemayor.service.billing;

import ing.software.dulcemayor.dto.billing.InvoiceResponseDTO;
import ing.software.dulcemayor.dto.billing.ManualPaymentConfirmDTO;
import ing.software.dulcemayor.dto.billing.MercadoPagoQrResponseDTO;
import ing.software.dulcemayor.dto.billing.OrderPaymentStatusDTO;
import ing.software.dulcemayor.dto.billing.PaymentConfigDTO;
import ing.software.dulcemayor.entity.actor.DeliveryAgent;
import ing.software.dulcemayor.entity.billing.Invoice;
import ing.software.dulcemayor.entity.billing.InvoiceDetail;
import ing.software.dulcemayor.entity.billing.Payment;
import ing.software.dulcemayor.entity.billing.PaymentMethod;
import ing.software.dulcemayor.entity.catalog.Stock;
import ing.software.dulcemayor.entity.logistics.DeliveryRoute;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.entity.order.OrderDetail;
import ing.software.dulcemayor.entity.order.OrderStatusHistory;
import ing.software.dulcemayor.enums.InvoiceStatus;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.enums.PaymentType;
import ing.software.dulcemayor.enums.RouteStatus;
import ing.software.dulcemayor.event.OrderPaidEvent;
import ing.software.dulcemayor.repository.actor.DeliveryAgentRepository;
import ing.software.dulcemayor.repository.billing.InvoiceRepository;
import ing.software.dulcemayor.repository.billing.PaymentMethodRepository;
import ing.software.dulcemayor.repository.billing.PaymentRepository;
import ing.software.dulcemayor.repository.catalog.StockRepository;
import ing.software.dulcemayor.repository.logistics.DeliveryRouteRepository;
import ing.software.dulcemayor.repository.order.OrderRepository;
import ing.software.dulcemayor.repository.order.OrderStatusHistoryRepository;
import ing.software.dulcemayor.service.order.OrderDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Handles idempotent payment accreditation, electronic invoicing, stock deduction,
 * automatic distribution route enablement, and payment status verification (polling + webhooks).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAccreditationService {

    private final OrderRepository orderRepository;
    private final StockRepository stockRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final DeliveryRouteRepository deliveryRouteRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final MercadoPagoService mercadoPagoService;
    private final QrCodeService qrCodeService;
    private final OrderDtoMapper orderDtoMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final PlatformTransactionManager transactionManager;

    private final ConcurrentHashMap<String, Object> orderLocks = new ConcurrentHashMap<>();

    @Value("${payment.bank.alias:DULCE.MAYOR.MZA}")
    private String bankAlias;

    @Value("${payment.bank.cbu:0000003100084920193842}")
    private String bankCbu;

    @Value("${payment.bank.holder:Dulce Mayor S.A.}")
    private String bankHolder;

    @Value("${payment.bank.name:Banco de la Nación Argentina}")
    private String bankName;

    public PaymentConfigDTO getPaymentConfig() {
        return PaymentConfigDTO.builder()
                .mercadoPagoEnabled(mercadoPagoService.isConfigured())
                .sandboxEnabled(mercadoPagoService.isSandboxEnabled())
                .publicKey(mercadoPagoService.getPublicKey())
                .bankAlias(bankAlias)
                .bankCbu(bankCbu)
                .bankHolder(bankHolder)
                .bankName(bankName)
                .build();
    }

    @Transactional
    public MercadoPagoQrResponseDTO generateOrFetchMercadoPagoQr(String orderId, String returnBaseUrl) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("No se puede generar QR para un pedido cancelado.");
        }
        if (isAlreadyAccreditedStatus(order.getStatus())) {
            throw new IllegalStateException("Este pedido ya se encuentra pago/acreditado.");
        }

        if (order.getMpInitPoint() == null || order.getMpInitPoint().isBlank()) {
            MercadoPagoService.PreferenceResult pref = mercadoPagoService.createPreference(order, returnBaseUrl);
            order.setMpPreferenceId(pref.preferenceId());
            order.setMpInitPoint(pref.initPoint());
            if (order.getPaymentType() != PaymentType.DIGITAL_WALLET) {
                order.setPaymentType(PaymentType.MERCADO_PAGO);
            }
            order.setStatus(OrderStatus.PENDING_PAYMENT);
            orderRepository.save(order);
        }

        String qrBase64 = qrCodeService.generateQrBase64(order.getMpInitPoint(), 320, 320);
        return MercadoPagoQrResponseDTO.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .total(order.getTotal())
                .preferenceId(order.getMpPreferenceId())
                .initPoint(order.getMpInitPoint())
                .qrPayload(order.getMpInitPoint())
                .qrImageBase64(qrBase64)
                .build();
    }

    public OrderPaymentStatusDTO verifyOrPollOrderStatus(String orderId, String paymentIdParam) {
        PollCheckContext ctx = executeReadOnly(() -> {
            Order order = loadOrderWithDetails(orderId);
            Invoice existingInvoice = invoiceRepository.findByOrderIdAndIsDeletedFalse(order.getId()).orElse(null);

            if (isAlreadyAccreditedStatus(order.getStatus()) && existingInvoice != null) {
                return new PollCheckContext(order, buildStatusDto(order, existingInvoice, "Pago acreditado y listo para distribución."));
            }
            if (order.getStatus() == OrderStatus.CANCELLED) {
                return new PollCheckContext(order, buildStatusDto(order, existingInvoice, "El pedido se encuentra cancelado."));
            }
            return new PollCheckContext(order, null);
        });

        if (ctx.immediateResult() != null) {
            return ctx.immediateResult();
        }

        Order order = ctx.order();
        Optional<MercadoPagoService.PaymentVerificationResult> verification = Optional.empty();
        if (paymentIdParam != null && !paymentIdParam.isBlank()) {
            try {
                long paymentId = Long.parseLong(paymentIdParam.trim());
                MercadoPagoService.PaymentVerificationResult direct = mercadoPagoService.verifyPaymentById(paymentId, order);
                if (direct.approved()) {
                    verification = Optional.of(direct);
                }
            } catch (NumberFormatException ex) {
                log.warn("Invalid numeric paymentId '{}' for order {}", paymentIdParam, orderId);
            }
        }

        if (verification.isEmpty()
                && order.getStatus() == OrderStatus.PENDING_PAYMENT
                && (order.getPaymentType() == PaymentType.MERCADO_PAGO
                || order.getPaymentType() == PaymentType.DIGITAL_WALLET
                || paymentIdParam != null)) {
            verification = mercadoPagoService.findApprovedPaymentForOrder(order);
        }

        if (verification.isPresent() && verification.get().approved()) {
            MercadoPagoService.PaymentVerificationResult approved = verification.get();
            PaymentType type = order.getPaymentType() == PaymentType.DIGITAL_WALLET
                    ? PaymentType.DIGITAL_WALLET
                    : PaymentType.MERCADO_PAGO;
            return accreditPaidOrder(
                    order.getId(),
                    type,
                    approved.mpPaymentId(),
                    approved.status(),
                    "MP-" + approved.mpPaymentId(),
                    "Pago acreditado vía Mercado Pago"
            );
        }

        return executeReadOnly(() -> {
            Order refreshed = loadOrderWithDetails(orderId);
            Invoice refreshedInvoice = invoiceRepository.findByOrderIdAndIsDeletedFalse(orderId).orElse(null);
            String msg = refreshed.getStatus() == OrderStatus.PENDING_PAYMENT
                    ? "Esperando confirmación del pago..."
                    : "Estado actual del pedido: " + refreshed.getStatus().name();
            return buildStatusDto(refreshed, refreshedInvoice, msg);
        });
    }

    public void handleMercadoPagoWebhook(String topic, String idParam, String dataIdParam, Map<String, Object> body) {
        try {
            String effectiveTopic = topic;
            String paymentIdText = dataIdParam != null && !dataIdParam.isBlank() ? dataIdParam : idParam;

            if (body != null) {
                if ((effectiveTopic == null || effectiveTopic.isBlank()) && body.get("type") != null) {
                    effectiveTopic = body.get("type").toString();
                }
                if ((effectiveTopic == null || effectiveTopic.isBlank()) && body.get("topic") != null) {
                    effectiveTopic = body.get("topic").toString();
                }
                if ((paymentIdText == null || paymentIdText.isBlank()) && body.get("data") instanceof Map<?, ?> dataMap) {
                    Object dataId = dataMap.get("id");
                    if (dataId != null) {
                        paymentIdText = dataId.toString();
                    }
                }
                if ((paymentIdText == null || paymentIdText.isBlank()) && body.get("id") != null) {
                    paymentIdText = body.get("id").toString();
                }
            }

            if (paymentIdText == null || paymentIdText.isBlank()) {
                log.debug("Ignoring MP webhook without payment id (topic={})", effectiveTopic);
                return;
            }
            if (effectiveTopic != null && !effectiveTopic.isBlank()
                    && !"payment".equalsIgnoreCase(effectiveTopic)) {
                log.debug("Ignoring non-payment MP webhook topic={}", effectiveTopic);
                return;
            }

            long paymentId = Long.parseLong(paymentIdText.trim());
            var rawPaymentOpt = mercadoPagoService.fetchRawPaymentById(paymentId);
            if (rawPaymentOpt.isEmpty()) {
                log.warn("MP webhook payment {} could not be fetched from Mercado Pago API", paymentId);
                return;
            }

            MercadoPagoService.MpPaymentInfo mpPayment = rawPaymentOpt.get();
            String extRef = mpPayment.externalReference();
            Optional<Order> targetOrder = (extRef != null && !extRef.isBlank())
                    ? orderRepository.findByIdAndIsDeletedFalse(extRef)
                    : orderRepository.findByMpPaymentIdAndIsDeletedFalse(String.valueOf(paymentId));

            if (targetOrder.isEmpty()) {
                log.warn("MP webhook payment {} has unknown external_reference={}", paymentId, extRef);
                return;
            }

            Order order = targetOrder.get();
            MercadoPagoService.PaymentVerificationResult verified = mercadoPagoService.evaluatePayment(mpPayment, order);
            if (!verified.approved()) {
                log.info("MP webhook payment {} for order {} not approved (status={})",
                        paymentId, order.getId(), verified.status());
                return;
            }

            PaymentType type = order.getPaymentType() == PaymentType.DIGITAL_WALLET
                    ? PaymentType.DIGITAL_WALLET
                    : PaymentType.MERCADO_PAGO;
            accreditPaidOrder(
                    order.getId(),
                    type,
                    verified.mpPaymentId(),
                    verified.status(),
                    "MP-" + verified.mpPaymentId(),
                    "Pago acreditado vía webhook Mercado Pago"
            );
        } catch (Exception ex) {
            log.warn("Failed processing MP webhook cleanly: {}", ex.getMessage());
        }
    }

    public OrderPaymentStatusDTO confirmManualPayment(String orderId, ManualPaymentConfirmDTO request) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("No se puede acreditar un pedido cancelado.");
        }

        PaymentType paymentType = order.getPaymentType() != null ? order.getPaymentType() : PaymentType.BANK_TRANSFER;
        if (request != null && request.getPaymentType() != null && !request.getPaymentType().isBlank()) {
            paymentType = PaymentType.valueOf(request.getPaymentType().trim().toUpperCase());
        }

        String reference = (request != null && request.getReference() != null && !request.getReference().isBlank())
                ? request.getReference().trim()
                : "MANUAL-" + order.getOrderNumber();
        String note = (request != null && request.getNote() != null && !request.getNote().isBlank())
                ? request.getNote().trim()
                : "Confirmación administrativa de pago (" + paymentType.name() + ")";

        return accreditPaidOrder(orderId, paymentType, order.getMpPaymentId(), "approved_manual", reference, note);
    }

    /**
     * Thread-safe & idempotent payment accreditation workflow.
     */
    public OrderPaymentStatusDTO accreditPaidOrder(
            String orderId,
            PaymentType paymentType,
            String mpPaymentId,
            String mpStatus,
            String reference,
            String note
    ) {
        Object lock = orderLocks.computeIfAbsent(orderId, k -> new Object());
        synchronized (lock) {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            AccreditationOutcome outcome = template.execute(status ->
                    executeAccreditationInTx(orderId, paymentType, mpPaymentId, mpStatus, reference, note));
            if (outcome != null && outcome.newlyAccredited && outcome.event != null) {
                eventPublisher.publishEvent(outcome.event);
            }
            return outcome != null ? outcome.statusDto : null;
        }
    }

    @Transactional
    public void deductStockAndInvoiceOnOrderInvoiced(Order order, OrderStatusHistory historyNoteSource) {
        deductStockIdempotently(order);
        PaymentType type = order.getPaymentType() != null ? order.getPaymentType() : PaymentType.CASH;
        Invoice invoice = createInvoiceIfMissing(order);
        if (paymentRepository.findByInvoiceIdAndIsDeletedFalse(invoice.getId()).isEmpty()
                && type != PaymentType.CASH) {
            recordPaymentIfMissing(invoice, type, order.getTotal(),
                    "INV-" + invoice.getInvoiceNumber(), "Facturación de pedido #" + order.getOrderNumber());
        }
        enableDistributionRoute(order);
        order.setReadyForDispatch(true);
        if (order.getDispatchEnabledAt() == null) {
            order.setDispatchEnabledAt(LocalDateTime.now());
        }
        orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public InvoiceResponseDTO getInvoiceForOrder(String orderId) {
        Order order = loadOrderWithDetails(orderId);
        Invoice invoice = invoiceRepository.findByOrderIdAndIsDeletedFalse(order.getId())
                .orElseThrow(() -> new IllegalStateException("El pedido todavía no posee factura emitida."));
        List<Payment> payments = paymentRepository.findByInvoiceIdAndIsDeletedFalse(invoice.getId());
        Payment primaryPayment = payments.isEmpty() ? null : payments.get(0);

        List<InvoiceResponseDTO.InvoiceDetailDTO> detailDtos = invoice.getInvoiceDetails().stream()
                .map(d -> InvoiceResponseDTO.InvoiceDetailDTO.builder()
                        .id(d.getId())
                        .productId(d.getProduct() != null ? d.getProduct().getId() : null)
                        .productCode(d.getProductCode())
                        .productName(d.getProductName())
                        .quantity(d.getQuantity())
                        .unitPrice(d.getUnitPrice())
                        .subtotal(d.getSubtotal())
                        .build())
                .toList();

        return InvoiceResponseDTO.builder()
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getInvoiceDate())
                .total(invoice.getTotal())
                .status(invoice.getStatus())
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerName(orderDtoMapper.resolveRetailerName(order))
                .storeName(order.getPointOfSale() != null ? order.getPointOfSale().getStoreName() : null)
                .paymentType(order.getPaymentType() != null ? order.getPaymentType().name() : null)
                .paymentReference(primaryPayment != null ? primaryPayment.getReference() : order.getMpPaymentId())
                .details(detailDtos)
                .build();
    }

    private AccreditationOutcome executeAccreditationInTx(
            String orderId,
            PaymentType paymentType,
            String mpPaymentId,
            String mpStatus,
            String reference,
            String note
    ) {
        Order order = loadOrderWithDetails(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot accredit cancelled order " + orderId);
        }

        Optional<Invoice> existingInvoiceOpt = invoiceRepository.findByOrderIdAndIsDeletedFalse(order.getId());
        if (existingInvoiceOpt.isPresent()
                && order.isStockDeducted()
                && isAlreadyAccreditedStatus(order.getStatus())) {
            OrderPaymentStatusDTO alreadyDto = buildStatusDto(order, existingInvoiceOpt.get(),
                    "Pago ya acreditado previamente.");
            return new AccreditationOutcome(alreadyDto, false, null);
        }

        deductStockIdempotently(order);

        PaymentType effectivePaymentType = paymentType != null
                ? paymentType
                : (order.getPaymentType() != null ? order.getPaymentType() : PaymentType.MERCADO_PAGO);
        order.setPaymentType(effectivePaymentType);
        if (mpPaymentId != null && !mpPaymentId.isBlank()) {
            order.setMpPaymentId(mpPaymentId);
        }
        order.setMpPaymentStatus(mpStatus != null && !mpStatus.isBlank() ? mpStatus : "approved");
        if (order.getPaidAt() == null) {
            order.setPaidAt(LocalDateTime.now());
        }
        if (note != null && !note.isBlank()) {
            order.setPaymentNote(note);
        }

        OrderStatus targetStatus = resolveAccreditedOrderStatus(order.getStatus());
        order.setStatus(targetStatus);
        order.setReadyForDispatch(true);
        if (order.getDispatchEnabledAt() == null) {
            order.setDispatchEnabledAt(LocalDateTime.now());
        }

        enableDistributionRoute(order);
        Order savedOrder = orderRepository.save(order);

        Invoice invoice = existingInvoiceOpt.orElseGet(() -> createInvoiceIfMissing(savedOrder));
        String effectiveReference = (reference != null && !reference.isBlank())
                ? reference
                : (mpPaymentId != null && !mpPaymentId.isBlank() ? mpPaymentId : "PAY-" + savedOrder.getOrderNumber());
        recordPaymentIfMissing(invoice, effectivePaymentType, savedOrder.getTotal(), effectiveReference, note);

        historyRepository.save(OrderStatusHistory.builder()
                .order(savedOrder)
                .status(OrderStatus.PAID)
                .dateTime(LocalDateTime.now())
                .note("Pago acreditado (" + effectivePaymentType.name() + ", Ref: " + effectiveReference
                        + "). Factura #" + invoice.getInvoiceNumber() + " emitida y habilitado para distribución.")
                .build());

        OrderPaidEvent event = OrderPaidEvent.builder()
                .orderId(savedOrder.getId())
                .orderNumber(savedOrder.getOrderNumber())
                .invoiceNumber(invoice.getInvoiceNumber())
                .paymentType(effectivePaymentType)
                .total(savedOrder.getTotal())
                .customerName(orderDtoMapper.resolveRetailerName(savedOrder))
                .customerEmail(savedOrder.getCustomerEmail())
                .storeName(savedOrder.getPointOfSale() != null ? savedOrder.getPointOfSale().getStoreName() : null)
                .deliveryAddress(orderDtoMapper.resolveDeliveryAddress(savedOrder))
                .paymentReference(effectiveReference)
                .paidAt(savedOrder.getPaidAt())
                .build();

        OrderPaymentStatusDTO dto = buildStatusDto(savedOrder, invoice,
                "Pago acreditado, factura emitida y orden enviada a distribución.");
        return new AccreditationOutcome(dto, true, event);
    }

    private void deductStockIdempotently(Order order) {
        if (order.isStockDeducted()) {
            return;
        }
        for (OrderDetail detail : order.getOrderDetails()) {
            if (detail.isDeleted()) {
                continue;
            }
            Stock stock = stockRepository.findByProductIdAndIsDeletedFalse(detail.getProduct().getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "No existe registro de stock para el producto " + detail.getProduct().getName()));
            if (stock.getCurrentQuantity() < detail.getQuantity()) {
                throw new IllegalStateException("Stock insuficiente al acreditar el producto '"
                        + detail.getProduct().getName() + "'. Disponible: " + stock.getCurrentQuantity()
                        + ", requerido: " + detail.getQuantity());
            }
            stock.setCurrentQuantity(stock.getCurrentQuantity() - detail.getQuantity());
            stock.setNote("Stock deducted for paid/invoiced order #" + order.getOrderNumber());
            stockRepository.save(stock);
        }
        order.setStockDeducted(true);
    }

    private Invoice createInvoiceIfMissing(Order order) {
        return invoiceRepository.findByOrderIdAndIsDeletedFalse(order.getId())
                .orElseGet(() -> {
                    Long maxNumber = invoiceRepository.findMaxInvoiceNumber();
                    long nextNumber = (maxNumber != null ? maxNumber : 10000L) + 1L;
                    Invoice invoice = Invoice.builder()
                            .invoiceNumber(nextNumber)
                            .invoiceDate(LocalDateTime.now())
                            .total(order.getTotal())
                            .status(InvoiceStatus.PAID)
                            .order(order)
                            .build();

                    for (OrderDetail orderDetail : order.getOrderDetails()) {
                        InvoiceDetail detail = InvoiceDetail.builder()
                                .quantity(orderDetail.getQuantity())
                                .unitPrice(orderDetail.getUnitPrice())
                                .subtotal(orderDetail.getSubtotal())
                                .product(orderDetail.getProduct())
                                .productCode(orderDetail.getProduct().getCode())
                                .productName(orderDetail.getProduct().getName())
                                .invoice(invoice)
                                .build();
                        invoice.getInvoiceDetails().add(detail);
                    }
                    return invoiceRepository.save(invoice);
                });
    }

    private void recordPaymentIfMissing(
            Invoice invoice,
            PaymentType paymentType,
            double amount,
            String reference,
            String note
    ) {
        if (!paymentRepository.findByInvoiceIdAndIsDeletedFalse(invoice.getId()).isEmpty()) {
            return;
        }
        PaymentMethod method = paymentMethodRepository.findByPaymentTypeAndIsDeletedFalse(paymentType)
                .orElseGet(() -> paymentMethodRepository.save(PaymentMethod.builder()
                        .paymentType(paymentType)
                        .note(paymentType.name())
                        .build()));

        Payment payment = Payment.builder()
                .amount(amount)
                .paymentDate(LocalDateTime.now())
                .reference(reference)
                .note(note)
                .invoice(invoice)
                .paymentMethod(method)
                .build();
        paymentRepository.save(payment);
    }

    private void enableDistributionRoute(Order order) {
        if (order.getDeliveryRoute() != null) {
            return;
        }
        List<DeliveryRoute> todayRoutes = deliveryRouteRepository.findByDateAndIsDeletedFalse(LocalDate.now());
        Optional<DeliveryRoute> selectedRoute = todayRoutes.stream().findFirst()
                .or(() -> deliveryRouteRepository.findFirstByIsDeletedFalseOrderByDateDesc());

        if (selectedRoute.isEmpty()) {
            List<DeliveryAgent> agents = deliveryAgentRepository.findByIsDeletedFalse();
            if (!agents.isEmpty()) {
                DeliveryRoute created = deliveryRouteRepository.save(DeliveryRoute.builder()
                        .date(LocalDate.now())
                        .status(RouteStatus.PENDING)
                        .deliveryAgent(agents.get(0))
                        .currentStop(order.getPointOfSale())
                        .build());
                selectedRoute = Optional.of(created);
            }
        }
        selectedRoute.ifPresent(order::setDeliveryRoute);
    }

    private Order loadOrderWithDetails(String orderId) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));
        if (order.getOrderDetails() != null) {
            order.getOrderDetails().size();
        }
        if (order.getPointOfSale() != null) {
            order.getPointOfSale().getStoreName();
            if (order.getPointOfSale().getRetailer() != null) {
                order.getPointOfSale().getRetailer().getFirstName();
            }
            if (order.getPointOfSale().getAddress() != null) {
                order.getPointOfSale().getAddress().getStreet();
            }
        }
        return order;
    }

    private OrderPaymentStatusDTO buildStatusDto(Order order, Invoice invoice, String message) {
        boolean paid = isAlreadyAccreditedStatus(order.getStatus()) && invoice != null;
        return OrderPaymentStatusDTO.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getStatus())
                .paymentType(order.getPaymentType())
                .paid(paid)
                .readyForDispatch(order.isReadyForDispatch())
                .stockDeducted(order.isStockDeducted())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .total(order.getTotal())
                .mpPreferenceId(order.getMpPreferenceId())
                .mpInitPoint(order.getMpInitPoint())
                .mpPaymentId(order.getMpPaymentId())
                .mpPaymentStatus(order.getMpPaymentStatus())
                .paidAt(order.getPaidAt())
                .invoiceId(invoice != null ? invoice.getId() : null)
                .invoiceNumber(invoice != null ? invoice.getInvoiceNumber() : null)
                .storeName(order.getPointOfSale() != null ? order.getPointOfSale().getStoreName() : null)
                .customerName(orderDtoMapper.resolveRetailerName(order))
                .customerEmail(order.getCustomerEmail())
                .deliveryAddress(orderDtoMapper.resolveDeliveryAddress(order))
                .deliveryCity(order.getDeliveryCity())
                .bankAlias(order.getPaymentType() == PaymentType.BANK_TRANSFER ? bankAlias : null)
                .bankCbu(order.getPaymentType() == PaymentType.BANK_TRANSFER ? bankCbu : null)
                .bankHolder(order.getPaymentType() == PaymentType.BANK_TRANSFER ? bankHolder : null)
                .bankName(order.getPaymentType() == PaymentType.BANK_TRANSFER ? bankName : null)
                .items(orderDtoMapper.toDetailDtos(order.getOrderDetails()))
                .message(message)
                .build();
    }

    private boolean isAlreadyAccreditedStatus(OrderStatus status) {
        return status == OrderStatus.PAID
                || status == OrderStatus.INVOICED
                || status == OrderStatus.IN_DELIVERY
                || status == OrderStatus.DELIVERED;
    }

    private OrderStatus resolveAccreditedOrderStatus(OrderStatus current) {
        if (current == OrderStatus.INVOICED || current == OrderStatus.IN_DELIVERY || current == OrderStatus.DELIVERED) {
            return current;
        }
        return OrderStatus.PAID;
    }

    private <T> T executeReadOnly(Supplier<T> work) {
        TransactionTemplate readTemplate = new TransactionTemplate(transactionManager);
        readTemplate.setReadOnly(true);
        return readTemplate.execute(status -> work.get());
    }

    private record PollCheckContext(
            Order order,
            OrderPaymentStatusDTO immediateResult
    ) {}

    private record AccreditationOutcome(
            OrderPaymentStatusDTO statusDto,
            boolean newlyAccredited,
            OrderPaidEvent event
    ) {}
}
