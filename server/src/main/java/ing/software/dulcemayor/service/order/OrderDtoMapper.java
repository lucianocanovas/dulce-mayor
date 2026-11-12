package ing.software.dulcemayor.service.order;

import ing.software.dulcemayor.dto.order.OrderDetailResponseDTO;
import ing.software.dulcemayor.dto.order.OrderResponseDTO;
import ing.software.dulcemayor.entity.billing.Invoice;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.entity.order.OrderDetail;
import ing.software.dulcemayor.enums.PaymentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Shared DTO mapper for Order entities and OrderDetails across order, billing, and route services.
 */
@Component
public class OrderDtoMapper {

    @Value("${payment.bank.alias:DULCE.MAYOR.MZA}")
    private String bankAlias;

    @Value("${payment.bank.cbu:0000003100084920193842}")
    private String bankCbu;

    @Value("${payment.bank.holder:Dulce Mayor S.A.}")
    private String bankHolder;

    public List<OrderDetailResponseDTO> toDetailDtos(List<OrderDetail> details) {
        if (details == null) {
            return List.of();
        }
        return details.stream()
                .filter(d -> !d.isDeleted())
                .map(d -> OrderDetailResponseDTO.builder()
                        .id(d.getId())
                        .productId(d.getProduct() != null ? d.getProduct().getId() : null)
                        .productCode(d.getProduct() != null ? d.getProduct().getCode() : "N/A")
                        .productName(d.getProduct() != null ? d.getProduct().getName() : "Producto eliminado")
                        .packaging(d.getProduct() != null ? d.getProduct().getPackaging() : "")
                        .quantity(d.getQuantity())
                        .unitPrice(d.getUnitPrice())
                        .subtotal(d.getSubtotal())
                        .build())
                .toList();
    }

    public OrderResponseDTO toOrderResponseDto(Order order, Invoice invoice) {
        String retailerName = resolveRetailerName(order);
        List<OrderDetailResponseDTO> items = toDetailDtos(order.getOrderDetails());

        OrderResponseDTO.OrderResponseDTOBuilder builder = OrderResponseDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderDateTime(order.getOrderDateTime())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .total(order.getTotal())
                .status(order.getStatus())
                .storeName(order.getPointOfSale() != null ? order.getPointOfSale().getStoreName() : null)
                .retailerName(retailerName)
                .salesRepName(order.getSalesRep() != null
                        ? order.getSalesRep().getFirstName() + " " + order.getSalesRep().getLastName()
                        : null)
                .couponCode(order.getCouponCode())
                .itemCount(items.size())
                .items(items)
                .paymentType(order.getPaymentType())
                .sourceChannel(order.getSourceChannel())
                .customerName(order.getCustomerName() != null ? order.getCustomerName() : retailerName)
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .deliveryAddress(resolveDeliveryAddress(order))
                .deliveryNeighborhood(order.getDeliveryNeighborhood())
                .deliveryCity(order.getDeliveryCity())
                .mpPreferenceId(order.getMpPreferenceId())
                .mpInitPoint(order.getMpInitPoint())
                .mpPaymentId(order.getMpPaymentId())
                .mpPaymentStatus(order.getMpPaymentStatus())
                .paidAt(order.getPaidAt())
                .stockDeducted(order.isStockDeducted())
                .readyForDispatch(order.isReadyForDispatch())
                .dispatchEnabledAt(order.getDispatchEnabledAt())
                .paymentNote(order.getPaymentNote());

        if (invoice != null) {
            builder.invoiceId(invoice.getId())
                    .invoiceNumber(invoice.getInvoiceNumber());
        }

        if (order.getPaymentType() == PaymentType.BANK_TRANSFER) {
            builder.bankAlias(bankAlias)
                    .bankCbu(bankCbu)
                    .bankHolder(bankHolder);
        }

        return builder.build();
    }

    public String resolveRetailerName(Order order) {
        if (order.getCustomerName() != null && !order.getCustomerName().isBlank()) {
            return order.getCustomerName();
        }
        if (order.getPointOfSale() != null && order.getPointOfSale().getRetailer() != null) {
            return (order.getPointOfSale().getRetailer().getFirstName() + " "
                    + order.getPointOfSale().getRetailer().getLastName()).trim();
        }
        return "Cliente";
    }

    public String resolveDeliveryAddress(Order order) {
        if (order.getDeliveryAddress() != null && !order.getDeliveryAddress().isBlank()) {
            return order.getDeliveryAddress();
        }
        if (order.getPointOfSale() != null && order.getPointOfSale().getAddress() != null) {
            var addr = order.getPointOfSale().getAddress();
            return ((addr.getStreet() != null ? addr.getStreet() : "") + " "
                    + (addr.getStreetNumber() != null ? addr.getStreetNumber() : "")).trim();
        }
        return null;
    }
}
