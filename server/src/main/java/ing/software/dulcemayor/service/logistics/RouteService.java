package ing.software.dulcemayor.service.logistics;

import ing.software.dulcemayor.dto.logistics.DailyRouteDTO;
import ing.software.dulcemayor.dto.logistics.RouteStopDTO;
import ing.software.dulcemayor.dto.order.PointOfSaleResponseDTO;
import ing.software.dulcemayor.dto.order.UpdateOrderStatusDTO;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.enums.RouteStatus;
import ing.software.dulcemayor.enums.UserRole;
import ing.software.dulcemayor.repository.order.OrderRepository;
import ing.software.dulcemayor.service.order.OrderService;
import ing.software.dulcemayor.service.order.PointOfSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final PointOfSaleService pointOfSaleService;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Transactional(readOnly = true)
    public DailyRouteDTO getDailyRoute(String roleStr) {
        UserRole role = UserRole.PRESALE_AGENT;
        if (roleStr != null && roleStr.equalsIgnoreCase("DELIVERY_AGENT")) {
            role = UserRole.DELIVERY_AGENT;
        }

        List<PointOfSaleResponseDTO> allPos = pointOfSaleService.getAllPointsOfSale();
        List<RouteStopDTO> stops = new ArrayList<>();

        LocalTime baseTime = LocalTime.of(9, 0);
        int index = 1;

        for (PointOfSaleResponseDTO pos : allPos) {
            List<Order> posOrders = orderRepository.findByPointOfSaleIdAndIsDeletedFalse(pos.getId());
            Order activeOrder = posOrders.stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .findFirst()
                    .orElse(null);

            String stopStatus = "PENDING";
            String deliveredAt = null;
            String orderId = null;
            Double orderTotal = null;

            if (activeOrder != null) {
                orderId = activeOrder.getId();
                orderTotal = activeOrder.getTotal();
                if (activeOrder.getStatus() == OrderStatus.DELIVERED) {
                    stopStatus = "DELIVERED";
                    deliveredAt = activeOrder.getOrderDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                }
            }

            LocalTime estTime = baseTime.plusMinutes((long) (index - 1) * 45);
            String estimatedArrival = estTime.format(DateTimeFormatter.ofPattern("HH:mm"));

            stops.add(RouteStopDTO.builder()
                    .id("stop-" + pos.getId())
                    .orderIndex(index++)
                    .pointOfSale(pos)
                    .status(stopStatus)
                    .orderId(orderId)
                    .orderTotal(orderTotal)
                    .estimatedArrival(estimatedArrival)
                    .deliveredAt(deliveredAt)
                    .notes(pos.getHasActiveOrder() ? "Pedido programado para entrega hoy." : "Visita de preventa y reposición.")
                    .build());
        }

        return DailyRouteDTO.builder()
                .id("route-today")
                .routeCode("RUT-MZA-CENTRO-" + LocalDate.now().getDayOfMonth())
                .date(LocalDate.now().toString())
                .assignedAgentName(role == UserRole.DELIVERY_AGENT ? "Repartidor Dulce Mayor" : "Juan Preventista")
                .role(role)
                .status(RouteStatus.PENDING)
                .stops(stops)
                .build();
    }

    @Transactional
    public void markStopDelivered(String orderId) {
        if (orderId != null && !orderId.isBlank()) {
            orderService.updateOrderStatus(orderId, UpdateOrderStatusDTO.builder()
                    .newStatus(OrderStatus.DELIVERED)
                    .note("Entrega confirmada en punto de venta por repartidor móvil")
                    .build());
        }
    }
}
