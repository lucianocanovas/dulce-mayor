package ing.software.dulcemayor.service.order;

import ing.software.dulcemayor.dto.order.CreateOrderItemDTO;
import ing.software.dulcemayor.dto.order.CreateOrderRequestDTO;
import ing.software.dulcemayor.dto.order.OrderResponseDTO;
import ing.software.dulcemayor.dto.order.UpdateOrderStatusDTO;
import ing.software.dulcemayor.entity.actor.SalesRepresentative;
import ing.software.dulcemayor.entity.catalog.Product;
import ing.software.dulcemayor.entity.catalog.Promotion;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.entity.order.OrderDetail;
import ing.software.dulcemayor.entity.order.OrderStatusHistory;
import ing.software.dulcemayor.entity.order.PointOfSale;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.repository.actor.SalesRepresentativeRepository;
import ing.software.dulcemayor.repository.catalog.PriceValidityRepository;
import ing.software.dulcemayor.repository.catalog.ProductRepository;
import ing.software.dulcemayor.repository.catalog.PromotionRepository;
import ing.software.dulcemayor.repository.catalog.StockRepository;
import ing.software.dulcemayor.repository.order.OrderDetailRepository;
import ing.software.dulcemayor.repository.order.OrderRepository;
import ing.software.dulcemayor.repository.order.OrderStatusHistoryRepository;
import ing.software.dulcemayor.repository.order.PointOfSaleRepository;
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

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findByIsDeletedFalse().stream()
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

        long nextOrderNum = orderRepository.findMaxOrderNumber() + 1;

        Order order = Order.builder()
                .orderNumber(nextOrderNum)
                .orderDateTime(LocalDateTime.now())
                .status(OrderStatus.LOADED)
                .pointOfSale(pos)
                .salesRep(rep)
                .couponCode(request.getCouponCode())
                .subtotal(0.0)
                .discount(0.0)
                .total(0.0)
                .orderDetails(new ArrayList<>())
                .build();

        Order savedOrder = orderRepository.save(order);

        double subtotal = 0.0;
        int totalUnits = 0;

        if (request.getItems() != null) {
            for (CreateOrderItemDTO itemReq : request.getItems()) {
                if (itemReq.getQuantity() <= 0) continue;

                Product product = productRepository.findByIdAndIsDeletedFalse(itemReq.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemReq.getProductId()));

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
        }

        double discount = 0.0;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            final int finalTotalUnits = totalUnits;
            Promotion promo = promotionRepository.findActivePromotionByCode(request.getCouponCode().trim().toUpperCase(), LocalDate.now())
                    .orElse(null);
            if (promo != null && finalTotalUnits >= promo.getMinimumQuantity()) {
                discount = (subtotal * promo.getDiscountPercentage()) / 100.0;
            }
        }

        double total = Math.max(0.0, subtotal - discount);
        savedOrder.setSubtotal(subtotal);
        savedOrder.setDiscount(discount);
        savedOrder.setTotal(total);

        Order finalOrder = orderRepository.save(savedOrder);

        // Record initial status history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .status(OrderStatus.LOADED)
                .dateTime(LocalDateTime.now())
                .note("Pedido registrado desde aplicación móvil de preventa/cliente")
                .order(finalOrder)
                .build();
        orderStatusHistoryRepository.save(history);

        return toResponseDto(finalOrder);
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

        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        // Record status history audit
        OrderStatusHistory history = OrderStatusHistory.builder()
                .status(newStatus)
                .dateTime(LocalDateTime.now())
                .note(request.getNote() != null ? request.getNote() : "Status updated to " + newStatus)
                .order(savedOrder)
                .build();
        orderStatusHistoryRepository.save(history);

        // Deduct inventory when order is INVOICED
        if (newStatus == OrderStatus.INVOICED) {
            for (OrderDetail detail : order.getOrderDetails()) {
                stockRepository.findByProductIdAndIsDeletedFalse(detail.getProduct().getId())
                        .ifPresent(stock -> {
                            int updatedQty = Math.max(0, stock.getCurrentQuantity() - detail.getQuantity());
                            stock.setCurrentQuantity(updatedQty);
                            stock.setNote("Stock deducted for invoice on order #" + order.getOrderNumber());
                            stockRepository.save(stock);
                        });
            }
        }

        return toResponseDto(savedOrder);
    }

    public OrderResponseDTO toResponseDto(Order order) {
        String storeName = order.getPointOfSale() != null ? order.getPointOfSale().getStoreName() : "N/A";
        String retailerName = (order.getPointOfSale() != null && order.getPointOfSale().getRetailer() != null)
                ? order.getPointOfSale().getRetailer().getFirstName() + " " + order.getPointOfSale().getRetailer().getLastName()
                : "N/A";
        String repName = order.getSalesRep() != null
                ? order.getSalesRep().getFirstName() + " " + order.getSalesRep().getLastName()
                : "Unassigned";

        int itemCount = order.getOrderDetails() != null ? order.getOrderDetails().size() : 0;

        return OrderResponseDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderDateTime(order.getOrderDateTime())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .total(order.getTotal())
                .status(order.getStatus())
                .storeName(storeName)
                .retailerName(retailerName)
                .salesRepName(repName)
                .couponCode(order.getCouponCode())
                .itemCount(itemCount)
                .build();
    }
}
