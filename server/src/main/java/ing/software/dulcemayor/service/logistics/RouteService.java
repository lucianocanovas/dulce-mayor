package ing.software.dulcemayor.service.logistics;

import ing.software.dulcemayor.dto.logistics.DailyRouteDTO;
import ing.software.dulcemayor.dto.logistics.RouteStopDTO;
import ing.software.dulcemayor.dto.order.PointOfSaleResponseDTO;
import ing.software.dulcemayor.dto.order.UpdateOrderStatusDTO;
import ing.software.dulcemayor.entity.billing.Invoice;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.enums.RouteStatus;
import ing.software.dulcemayor.enums.UserRole;
import ing.software.dulcemayor.event.OrderDeliveredEvent;
import ing.software.dulcemayor.repository.billing.InvoiceRepository;
import ing.software.dulcemayor.repository.order.OrderRepository;
import ing.software.dulcemayor.service.order.OrderDtoMapper;
import ing.software.dulcemayor.service.order.OrderService;
import ing.software.dulcemayor.service.order.PointOfSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final PointOfSaleService pointOfSaleService;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final OrderService orderService;
    private final OrderDtoMapper orderDtoMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public DailyRouteDTO getDailyRoute(String roleStr) {
        UserRole role = UserRole.PRESALE_AGENT;
        if (roleStr != null && roleStr.equalsIgnoreCase("DELIVERY_AGENT")) {
            role = UserRole.DELIVERY_AGENT;
        }

        List<RouteStopDTO> stops = (role == UserRole.DELIVERY_AGENT)
                ? buildDeliveryStops()
                : buildPresaleStops();

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

    private List<RouteStopDTO> buildDeliveryStops() {
        List<Order> dispatchOrders = orderRepository.findByIsDeletedFalseOrderByOrderDateTimeDesc().stream()
                .filter(this::isOrderVisibleForDelivery)
                .sorted(Comparator
                        .comparing((Order o) -> o.getStatus() == OrderStatus.DELIVERED)
                        .thenComparing(Order::getOrderDateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<RouteStopDTO> stops = new ArrayList<>();
        LocalTime baseTime = LocalTime.of(9, 0);
        int index = 1;

        for (Order order : dispatchOrders) {
            PointOfSaleResponseDTO posDto = pointOfSaleService.toResponseDTO(order.getPointOfSale());
            if (order.getDeliveryAddress() != null && !order.getDeliveryAddress().isBlank()) {
                posDto.setAddress(order.getDeliveryAddress());
            }
            if (order.getDeliveryNeighborhood() != null && !order.getDeliveryNeighborhood().isBlank()) {
                posDto.setNeighborhood(order.getDeliveryNeighborhood());
            }
            if (order.getDeliveryCity() != null && !order.getDeliveryCity().isBlank()) {
                posDto.setCity(order.getDeliveryCity());
            }
            if (order.getCustomerName() != null && !order.getCustomerName().isBlank()) {
                posDto.setOwnerName(order.getCustomerName());
            }

            Invoice invoice = invoiceRepository.findByOrderIdAndIsDeletedFalse(order.getId()).orElse(null);
            boolean isDelivered = order.getStatus() == OrderStatus.DELIVERED;
            boolean isPaid = order.getStatus() == OrderStatus.PAID
                    || invoice != null;

            String deliveredAt = isDelivered && order.getOrderDateTime() != null
                    ? order.getOrderDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                    : null;
            String estimatedArrival = baseTime.plusMinutes((long) (index - 1) * 35)
                    .format(DateTimeFormatter.ofPattern("HH:mm"));

            String channelLabel = "LANDING".equalsIgnoreCase(order.getSourceChannel())
                    ? "Tienda Web"
                    : "Preventa Campo";

            String note = isPaid
                    ? String.format("Pedido #%d (%s) pagado • Factura #%s lista para entrega.",
                            order.getOrderNumber(),
                            channelLabel,
                            invoice != null ? invoice.getInvoiceNumber() : "-")
                    : String.format("Pedido #%d (%s) • Cobro contra entrega (%s).",
                            order.getOrderNumber(),
                            channelLabel,
                            order.getPaymentType() != null ? order.getPaymentType().name() : "CASH");

            stops.add(RouteStopDTO.builder()
                    .id("stop-order-" + order.getId())
                    .orderIndex(index++)
                    .pointOfSale(posDto)
                    .status(isDelivered ? "DELIVERED" : "PENDING")
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .orderTotal(order.getTotal())
                    .estimatedArrival(estimatedArrival)
                    .deliveredAt(deliveredAt)
                    .notes(note)
                    .paymentType(order.getPaymentType() != null ? order.getPaymentType().name() : "CASH")
                    .paid(isPaid)
                    .readyForDispatch(order.isReadyForDispatch())
                    .invoiceNumber(invoice != null ? invoice.getInvoiceNumber() : null)
                    .sourceChannel(order.getSourceChannel())
                    .customerName(orderDtoMapper.resolveRetailerName(order))
                    .mpInitPoint(order.getMpInitPoint())
                    .items(orderDtoMapper.toDetailDtos(order.getOrderDetails()))
                    .build());
        }
        return stops;
    }

    private List<RouteStopDTO> buildPresaleStops() {
        List<PointOfSaleResponseDTO> allPos = pointOfSaleService.getAllPointsOfSale();
        List<RouteStopDTO> stops = new ArrayList<>();
        LocalTime baseTime = LocalTime.of(9, 0);
        int index = 1;

        for (PointOfSaleResponseDTO pos : allPos) {
            List<Order> posOrders = orderRepository.findByPointOfSaleIdAndIsDeletedFalse(pos.getId()).stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .sorted(Comparator.comparing(Order::getOrderDateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();

            Order activeOrder = posOrders.stream()
                    .filter(o -> o.getStatus() != OrderStatus.DELIVERED)
                    .findFirst()
                    .orElse(posOrders.isEmpty() ? null : posOrders.get(0));

            String stopStatus = "PENDING";
            String deliveredAt = null;
            String orderId = null;
            Long orderNumber = null;
            Double orderTotal = null;
            String paymentType = null;
            boolean paid = false;
            boolean readyForDispatch = false;
            Long invoiceNumber = null;
            String mpInitPoint = null;
            String sourceChannel = null;
            String customerName = pos.getOwnerName();
            List<ing.software.dulcemayor.dto.order.OrderDetailResponseDTO> items = List.of();
            String notes = "Visita de preventa y reposición.";

            if (activeOrder != null) {
                orderId = activeOrder.getId();
                orderNumber = activeOrder.getOrderNumber();
                orderTotal = activeOrder.getTotal();
                paymentType = activeOrder.getPaymentType() != null ? activeOrder.getPaymentType().name() : "CASH";
                readyForDispatch = activeOrder.isReadyForDispatch();
                mpInitPoint = activeOrder.getMpInitPoint();
                sourceChannel = activeOrder.getSourceChannel();
                customerName = orderDtoMapper.resolveRetailerName(activeOrder);
                items = orderDtoMapper.toDetailDtos(activeOrder.getOrderDetails());

                Invoice invoice = invoiceRepository.findByOrderIdAndIsDeletedFalse(activeOrder.getId()).orElse(null);
                if (invoice != null) {
                    invoiceNumber = invoice.getInvoiceNumber();
                    paid = true;
                }

                if (activeOrder.getStatus() == OrderStatus.PENDING_PAYMENT) {
                    stopStatus = "PENDING_PAYMENT";
                    notes = "Pedido #" + activeOrder.getOrderNumber() + " pendiente de acreditación (" + paymentType + ").";
                } else if (activeOrder.getStatus() == OrderStatus.DELIVERED) {
                    stopStatus = "DELIVERED";
                    deliveredAt = activeOrder.getOrderDateTime() != null
                            ? activeOrder.getOrderDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                            : null;
                    notes = "Pedido #" + activeOrder.getOrderNumber() + " entregado.";
                } else {
                    notes = "Pedido #" + activeOrder.getOrderNumber()
                            + (paid ? " pagado y listo para entrega." : " programado para entrega.");
                }
            }

            String estimatedArrival = baseTime.plusMinutes((long) (index - 1) * 45)
                    .format(DateTimeFormatter.ofPattern("HH:mm"));

            stops.add(RouteStopDTO.builder()
                    .id("stop-" + pos.getId())
                    .orderIndex(index++)
                    .pointOfSale(pos)
                    .status(stopStatus)
                    .orderId(orderId)
                    .orderNumber(orderNumber)
                    .orderTotal(orderTotal)
                    .estimatedArrival(estimatedArrival)
                    .deliveredAt(deliveredAt)
                    .notes(notes)
                    .paymentType(paymentType)
                    .paid(paid)
                    .readyForDispatch(readyForDispatch)
                    .invoiceNumber(invoiceNumber)
                    .sourceChannel(sourceChannel)
                    .customerName(customerName)
                    .mpInitPoint(mpInitPoint)
                    .items(items)
                    .build());
        }
        return stops;
    }

    private boolean isOrderVisibleForDelivery(Order order) {
        if (order.isDeleted() || order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            return false;
        }
        return order.isReadyForDispatch()
                || order.getStatus() == OrderStatus.PAID
                || order.getStatus() == OrderStatus.INVOICED
                || order.getStatus() == OrderStatus.IN_DELIVERY
                || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.LOADED;
    }

    @Transactional
    public void markStopDelivered(String orderId) {
        if (orderId != null && !orderId.isBlank()) {
            orderService.updateOrderStatus(orderId, UpdateOrderStatusDTO.builder()
                    .newStatus(OrderStatus.DELIVERED)
                    .note("Entrega confirmada en punto de venta por repartidor móvil")
                    .build());

            orderRepository.findByIdAndIsDeletedFalse(orderId).ifPresent(deliveredOrder -> {
                Order nextPending = orderRepository.findByIsDeletedFalseOrderByOrderDateTimeDesc().stream()
                        .filter(o -> !o.getId().equals(orderId)
                                && isOrderVisibleForDelivery(o)
                                && o.getStatus() != OrderStatus.DELIVERED)
                        .findFirst()
                        .orElse(null);

                String nextStoreName = nextPending != null && nextPending.getPointOfSale() != null
                        ? nextPending.getPointOfSale().getStoreName()
                        : null;
                String nextStoreEmail = nextPending != null
                        ? (nextPending.getCustomerEmail() != null
                                ? nextPending.getCustomerEmail()
                                : (nextPending.getPointOfSale() != null ? nextPending.getPointOfSale().getContactEmail() : null))
                        : null;

                eventPublisher.publishEvent(OrderDeliveredEvent.builder()
                        .orderId(deliveredOrder.getId())
                        .orderNumber(deliveredOrder.getOrderNumber())
                        .storeName(deliveredOrder.getPointOfSale() != null ? deliveredOrder.getPointOfSale().getStoreName() : null)
                        .customerName(orderDtoMapper.resolveRetailerName(deliveredOrder))
                        .customerEmail(deliveredOrder.getCustomerEmail())
                        .deliveryAddress(orderDtoMapper.resolveDeliveryAddress(deliveredOrder))
                        .nextStopStoreName(nextStoreName)
                        .nextStopContactEmail(nextStoreEmail)
                        .deliveredAt(LocalDateTime.now())
                        .build());
            });
        }
    }
}
