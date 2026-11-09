package ing.software.dulcemayor.service.order;

import ing.software.dulcemayor.dto.order.OrderResponseDTO;
import ing.software.dulcemayor.dto.order.UpdateOrderStatusDTO;
import ing.software.dulcemayor.entity.catalog.Stock;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.entity.order.OrderDetail;
import ing.software.dulcemayor.entity.order.OrderStatusHistory;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.repository.order.OrderRepository;
import ing.software.dulcemayor.repository.order.OrderStatusHistoryRepository;
import ing.software.dulcemayor.repository.catalog.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final StockRepository stockRepository;

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
