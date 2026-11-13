package ing.software.dulcemayor.service.order;

import ing.software.dulcemayor.dto.billing.ManualPaymentConfirmDTO;
import ing.software.dulcemayor.dto.order.CreateOrderItemDTO;
import ing.software.dulcemayor.dto.order.CreateOrderRequestDTO;
import ing.software.dulcemayor.dto.order.OrderResponseDTO;
import ing.software.dulcemayor.dto.order.UpdateOrderStatusDTO;
import ing.software.dulcemayor.entity.actor.SalesRepresentative;
import ing.software.dulcemayor.entity.billing.Invoice;
import ing.software.dulcemayor.entity.catalog.Product;
import ing.software.dulcemayor.entity.catalog.Promotion;
import ing.software.dulcemayor.entity.catalog.Stock;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.entity.order.OrderDetail;
import ing.software.dulcemayor.entity.order.OrderStatusHistory;
import ing.software.dulcemayor.entity.order.PointOfSale;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.enums.PaymentType;
import ing.software.dulcemayor.repository.actor.SalesRepresentativeRepository;
import ing.software.dulcemayor.repository.billing.InvoiceRepository;
import ing.software.dulcemayor.repository.catalog.PriceValidityRepository;
import ing.software.dulcemayor.repository.catalog.ProductRepository;
import ing.software.dulcemayor.repository.catalog.PromotionRepository;
import ing.software.dulcemayor.repository.catalog.StockRepository;
import ing.software.dulcemayor.repository.order.OrderDetailRepository;
import ing.software.dulcemayor.repository.order.OrderRepository;
import ing.software.dulcemayor.repository.order.OrderStatusHistoryRepository;
import ing.software.dulcemayor.repository.order.PointOfSaleRepository;
import ing.software.dulcemayor.service.billing.PaymentAccreditationService;
import ing.software.dulcemayor.service.billing.strategy.PaymentInitiationResult;
import ing.software.dulcemayor.service.billing.strategy.PaymentStrategyFactory;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final StockRepository stockRepository;
    private final PointOfSaleRepository pointOfSaleRepository;
    private final SalesRepresentativeRepository salesRepRepository;
    private final ProductRepository productRepository;
    private final PriceValidityRepository priceValidityRepository;
    private final PromotionRepository promotionRepository;
    private final InvoiceRepository invoiceRepository;
    private final OrderDtoMapper orderDtoMapper;
    private final PaymentStrategyFactory paymentStrategyFactory;
    private final PaymentAccreditationService paymentAccreditationService;

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findByIsDeletedFalseOrderByOrderDateTimeDesc().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(String id) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));
        return toResponseDto(order);
    }

    @Transactional
    public OrderResponseDTO createOrder(CreateOrderRequestDTO request) {
        PointOfSale pos = pointOfSaleRepository.findByIdAndIsDeletedFalse(request.getPointOfSaleId())
                .orElseThrow(() -> new IllegalArgumentException("Point of Sale not found: " + request.getPointOfSaleId()));

        SalesRepresentative rep = null;
        if (request.getSalesRepId() != null && !request.getSalesRepId().isBlank()) {
            rep = salesRepRepository.findByIdAndIsDeletedFalse(request.getSalesRepId()).orElse(null);
        }
        if (rep == null) {
            rep = pos.getSalesRep();
        }
        if (rep == null) {
            rep = salesRepRepository.findAll().stream().findFirst().orElse(null);
        }

        PaymentType paymentType = resolvePaymentType(request.getPaymentType());
        String sourceChannel = (request.getSourceChannel() != null && !request.getSourceChannel().isBlank())
                ? request.getSourceChannel().trim()
                : "ORDER_MANAGEMENT";

        String customerName = pos.getRetailer() != null
                ? (pos.getRetailer().getFirstName() + " " + pos.getRetailer().getLastName()).trim()
                : pos.getStoreName();
        String deliveryAddr = pos.getAddress() != null ? pos.getAddress().getStreet() : null;
        String deliveryNeighborhood = pos.getAddress() != null ? pos.getAddress().getNeighborhood() : null;
        String deliveryCity = (pos.getAddress() != null && pos.getAddress().getCity() != null)
                ? pos.getAddress().getCity().getName()
                : "Mendoza Capital";

        OrderCreationOutcome outcome = buildAndInitiateOrder(OrderDraftSpec.builder()
                .pointOfSale(pos)
                .salesRep(rep)
                .couponCode(request.getCouponCode())
                .paymentType(paymentType)
                .sourceChannel(sourceChannel)
                .customerName(customerName)
                .deliveryAddress(deliveryAddr)
                .deliveryNeighborhood(deliveryNeighborhood)
                .deliveryCity(deliveryCity)
                .paymentNote(request.getPaymentNote())
                .items(request.getItems())
                .build());

        OrderResponseDTO dto = toResponseDto(outcome.order());
        if (outcome.initiationResult() != null) {
            dto.setQrImageBase64(outcome.initiationResult().getQrImageBase64());
        }
        return dto;
    }

    @Transactional
    public OrderCreationOutcome buildAndInitiateOrder(OrderDraftSpec spec) {
        if (spec.getItems() == null || spec.getItems().stream().noneMatch(i -> i.getQuantity() > 0)) {
            throw new IllegalArgumentException("El pedido debe incluir al menos un producto con cantidad mayor a cero.");
        }

        long nextOrderNum = orderRepository.findMaxOrderNumber() + 1;
        PaymentType paymentType = spec.getPaymentType() != null ? spec.getPaymentType() : PaymentType.CASH;
        OrderStatus tempStatus = (paymentType == PaymentType.MERCADO_PAGO
                || paymentType == PaymentType.DIGITAL_WALLET
                || paymentType == PaymentType.BANK_TRANSFER)
                ? OrderStatus.PENDING_PAYMENT
                : OrderStatus.LOADED;

        Order order = Order.builder()
                .orderNumber(nextOrderNum)
                .orderDateTime(LocalDateTime.now())
                .status(tempStatus)
                .paymentType(paymentType)
                .sourceChannel(spec.getSourceChannel())
                .customerName(spec.getCustomerName())
                .customerEmail(spec.getCustomerEmail())
                .customerPhone(spec.getCustomerPhone())
                .deliveryAddress(spec.getDeliveryAddress())
                .deliveryNeighborhood(spec.getDeliveryNeighborhood())
                .deliveryCity(spec.getDeliveryCity())
                .paymentNote(spec.getPaymentNote())
                .pointOfSale(spec.getPointOfSale())
                .salesRep(spec.getSalesRep())
                .couponCode(spec.getCouponCode())
                .subtotal(0.0)
                .discount(0.0)
                .total(0.0)
                .stockDeducted(false)
                .readyForDispatch(false)
                .orderDetails(new ArrayList<>())
                .build();

        Order savedOrder = orderRepository.save(order);

        double subtotal = 0.0;
        int totalUnits = 0;

        for (CreateOrderItemDTO itemReq : spec.getItems()) {
            if (itemReq.getQuantity() <= 0) {
                continue;
            }

            Product product = null;
            if (itemReq.getProductId() != null && !itemReq.getProductId().isBlank()) {
                product = productRepository.findByIdAndIsDeletedFalse(itemReq.getProductId()).orElse(null);
            }
            if (product == null && itemReq.getProductCode() != null && !itemReq.getProductCode().isBlank()) {
                product = productRepository.findByCodeAndIsDeletedFalse(itemReq.getProductCode().trim()).orElse(null);
            }
            if (product == null) {
                String ref = itemReq.getProductCode() != null ? itemReq.getProductCode() : itemReq.getProductId();
                throw new IllegalArgumentException("El producto solicitado ya no está disponible en el catálogo (" + ref
                        + "). Por favor vacíe su carrito y vuelva a agregarlo.");
            }

            Stock stock = stockRepository.findByProductIdAndIsDeletedFalse(product.getId()).orElse(null);
            int available = stock != null ? stock.getCurrentQuantity() : 0;
            if (available < itemReq.getQuantity()) {
                throw new IllegalStateException("Stock insuficiente para '" + product.getName()
                        + "'. Disponible: " + available + ", solicitado: " + itemReq.getQuantity());
            }

            double unitPrice = priceValidityRepository.findActivePriceByProductId(product.getId())
                    .map(pv -> pv.getPrice())
                    .orElse(10000.0);

            double lineTotal = unitPrice * itemReq.getQuantity();
            subtotal += lineTotal;
            totalUnits += itemReq.getQuantity();

            OrderDetail detail = OrderDetail.builder()
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(lineTotal)
                    .product(product)
                    .order(savedOrder)
                    .build();

            orderDetailRepository.save(detail);
            savedOrder.getOrderDetails().add(detail);
        }

        double discount = 0.0;
        if (spec.getCouponCode() != null && !spec.getCouponCode().isBlank()) {
            final int finalTotalUnits = totalUnits;
            Promotion promo = promotionRepository
                    .findActivePromotionByCode(spec.getCouponCode().trim().toUpperCase(), LocalDate.now())
                    .orElse(null);
            if (promo != null) {
                if (promo.getProduct() != null && promo.getProduct().getId() != null) {
                    String targetProductId = promo.getProduct().getId();
                    int matchingUnits = savedOrder.getOrderDetails().stream()
                            .filter(d -> d.getProduct() != null && targetProductId.equals(d.getProduct().getId()))
                            .mapToInt(OrderDetail::getQuantity)
                            .sum();
                    double matchingSubtotal = savedOrder.getOrderDetails().stream()
                            .filter(d -> d.getProduct() != null && targetProductId.equals(d.getProduct().getId()))
                            .mapToDouble(OrderDetail::getSubtotal)
                            .sum();
                    if (matchingUnits >= promo.getMinimumQuantity()) {
                        discount = (matchingSubtotal * promo.getDiscountPercentage()) / 100.0;
                    }
                } else if (finalTotalUnits >= promo.getMinimumQuantity()) {
                    discount = (subtotal * promo.getDiscountPercentage()) / 100.0;
                }
            }
        }

        double total = Math.max(0.0, subtotal - discount);
        savedOrder.setSubtotal(subtotal);
        savedOrder.setDiscount(discount);
        savedOrder.setTotal(total);
        savedOrder = orderRepository.save(savedOrder);

        PaymentInitiationResult initResult = paymentStrategyFactory
                .resolve(paymentType)
                .initiate(savedOrder, spec.getReturnBaseUrl());

        savedOrder.setStatus(initResult.getInitialOrderStatus());
        savedOrder.setReadyForDispatch(initResult.isReadyForDispatch());
        if (initResult.isReadyForDispatch()) {
            savedOrder.setDispatchEnabledAt(LocalDateTime.now());
        }
        if (initResult.getPreferenceId() != null) {
            savedOrder.setMpPreferenceId(initResult.getPreferenceId());
        }
        if (initResult.getInitPoint() != null) {
            savedOrder.setMpInitPoint(initResult.getInitPoint());
        }

        Order finalOrder = orderRepository.save(savedOrder);

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .status(finalOrder.getStatus())
                .dateTime(LocalDateTime.now())
                .note(initResult.getMessage() != null ? initResult.getMessage()
                        : "Pedido registrado (" + spec.getSourceChannel() + ")")
                .order(finalOrder)
                .build());

        return new OrderCreationOutcome(finalOrder, initResult);
    }

    @Transactional
    public OrderResponseDTO updateOrderStatus(String id, UpdateOrderStatusDTO request) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus = request.getNewStatus();

        if (oldStatus == newStatus) {
            return toResponseDto(order);
        }

        if (newStatus == OrderStatus.PAID) {
            paymentAccreditationService.confirmManualPayment(id, ManualPaymentConfirmDTO.builder()
                    .paymentType(order.getPaymentType() != null ? order.getPaymentType().name() : PaymentType.CASH.name())
                    .note(request.getNote() != null ? request.getNote() : "Pago confirmado desde panel central")
                    .build());
            return getOrderById(id);
        }

        order.setStatus(newStatus);
        if (newStatus == OrderStatus.IN_DELIVERY || newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.INVOICED) {
            order.setReadyForDispatch(true);
            if (order.getDispatchEnabledAt() == null) {
                order.setDispatchEnabledAt(LocalDateTime.now());
            }
        }
        Order savedOrder = orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .status(newStatus)
                .dateTime(LocalDateTime.now())
                .note(request.getNote() != null ? request.getNote() : "Status updated to " + newStatus)
                .order(savedOrder)
                .build();
        orderStatusHistoryRepository.save(history);

        if (newStatus == OrderStatus.INVOICED) {
            paymentAccreditationService.deductStockAndInvoiceOnOrderInvoiced(savedOrder, history);
        }

        return toResponseDto(savedOrder);
    }

    @Transactional
    public void deleteOrder(String id) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));
        order.setDeleted(true);
        order.setStatus(OrderStatus.CANCELLED);
        order.setReadyForDispatch(false);
        orderRepository.save(order);
    }

    public PaymentType resolvePaymentType(String raw) {
        if (raw == null || raw.isBlank()) {
            return PaymentType.CASH;
        }
        String normalized = raw.trim().toUpperCase();
        return switch (normalized) {
            case "MP", "MERCADOPAGO", "MERCADO_PAGO" -> PaymentType.MERCADO_PAGO;
            case "QR", "BILLETERA_VIRTUAL", "DIGITAL_WALLET" -> PaymentType.DIGITAL_WALLET;
            case "TRANSFER", "TRANSFERENCIA", "BANK_TRANSFER" -> PaymentType.BANK_TRANSFER;
            case "EFECTIVO", "CASH" -> PaymentType.CASH;
            default -> PaymentType.valueOf(normalized);
        };
    }

    public OrderResponseDTO toResponseDto(Order order) {
        Invoice invoice = invoiceRepository.findByOrderIdAndIsDeletedFalse(order.getId()).orElse(null);
        return orderDtoMapper.toOrderResponseDto(order, invoice);
    }

    @lombok.Getter
    @Builder
    public static class OrderDraftSpec {
        private PointOfSale pointOfSale;
        private SalesRepresentative salesRep;
        private String couponCode;
        private PaymentType paymentType;
        private String sourceChannel;
        private String customerName;
        private String customerEmail;
        private String customerPhone;
        private String deliveryAddress;
        private String deliveryNeighborhood;
        private String deliveryCity;
        private String paymentNote;
        private String returnBaseUrl;
        private List<CreateOrderItemDTO> items;
    }

    public record OrderCreationOutcome(Order order, PaymentInitiationResult initiationResult) {}
}
