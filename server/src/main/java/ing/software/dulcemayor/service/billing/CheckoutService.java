package ing.software.dulcemayor.service.billing;

import ing.software.dulcemayor.dto.billing.CheckoutRequestDTO;
import ing.software.dulcemayor.dto.billing.CheckoutResponseDTO;
import ing.software.dulcemayor.dto.order.OrderResponseDTO;
import ing.software.dulcemayor.entity.actor.Retailer;
import ing.software.dulcemayor.entity.location.Address;
import ing.software.dulcemayor.entity.location.City;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.entity.order.PointOfSale;
import ing.software.dulcemayor.enums.DocumentType;
import ing.software.dulcemayor.enums.PaymentType;
import ing.software.dulcemayor.repository.actor.RetailerRepository;
import ing.software.dulcemayor.repository.location.AddressRepository;
import ing.software.dulcemayor.repository.location.CityRepository;
import ing.software.dulcemayor.repository.order.OrderRepository;
import ing.software.dulcemayor.repository.order.PointOfSaleRepository;
import ing.software.dulcemayor.service.billing.strategy.PaymentInitiationResult;
import ing.software.dulcemayor.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles customer web checkouts originating from the Landing Storefront (:8081).
 */
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final RetailerRepository retailerRepository;
    private final AddressRepository addressRepository;
    private final CityRepository cityRepository;
    private final PointOfSaleRepository pointOfSaleRepository;

    @Transactional
    public CheckoutResponseDTO processLandingCheckout(CheckoutRequestDTO request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("El carrito de compras no puede estar vacío.");
        }
        if (request.getCustomerName() == null || request.getCustomerName().isBlank()) {
            throw new IllegalArgumentException("El nombre y apellido del comprador es obligatorio.");
        }
        if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank()) {
            throw new IllegalArgumentException("La dirección de entrega es obligatoria.");
        }

        PointOfSale pos = createOrResolveWebPointOfSale(request);
        PaymentType paymentType = orderService.resolvePaymentType(request.getPaymentType());

        OrderService.OrderCreationOutcome outcome = orderService.buildAndInitiateOrder(
                OrderService.OrderDraftSpec.builder()
                        .pointOfSale(pos)
                        .salesRep(null)
                        .couponCode(request.getCouponCode())
                        .paymentType(paymentType)
                        .sourceChannel("LANDING")
                        .customerName(request.getCustomerName().trim())
                        .customerEmail(request.getCustomerEmail() != null ? request.getCustomerEmail().trim() : null)
                        .customerPhone(request.getCustomerPhone() != null ? request.getCustomerPhone().trim() : null)
                        .deliveryAddress(request.getDeliveryAddress().trim())
                        .deliveryNeighborhood(request.getDeliveryNeighborhood() != null
                                ? request.getDeliveryNeighborhood().trim() : "Centro")
                        .deliveryCity(request.getDeliveryCity() != null && !request.getDeliveryCity().isBlank()
                                ? request.getDeliveryCity().trim() : "Mendoza Capital")
                        .paymentNote(request.getNotes())
                        .returnBaseUrl(request.getReturnBaseUrl())
                        .items(request.getItems())
                        .build()
        );

        Order order = outcome.order();
        PaymentInitiationResult init = outcome.initiationResult();

        return CheckoutResponseDTO.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentType(order.getPaymentType())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .total(order.getTotal())
                .preferenceId(init.getPreferenceId())
                .initPoint(init.getInitPoint())
                .qrPayload(init.getQrPayload())
                .qrImageBase64(init.getQrImageBase64())
                .bankAlias(init.getBankAlias())
                .bankCbu(init.getBankCbu())
                .bankHolder(init.getBankHolder())
                .bankName(init.getBankName())
                .readyForDispatch(order.isReadyForDispatch())
                .message(init.getMessage())
                .build();
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> findCustomerOrders(String email, Long orderNumber) {
        if (orderNumber != null) {
            return orderRepository.findByOrderNumberAndIsDeletedFalse(orderNumber)
                    .map(orderService::toResponseDto)
                    .map(List::of)
                    .orElse(List.of());
        }
        if (email != null && !email.isBlank()) {
            return orderRepository.findByCustomerEmailIgnoreCaseAndIsDeletedFalseOrderByOrderDateTimeDesc(email.trim())
                    .stream()
                    .map(orderService::toResponseDto)
                    .toList();
        }
        return List.of();
    }

    private PointOfSale createOrResolveWebPointOfSale(CheckoutRequestDTO request) {
        String fullName = request.getCustomerName().trim();
        String[] parts = fullName.split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "Web";
        String doc = (request.getTaxIdOrDni() != null && !request.getTaxIdOrDni().isBlank())
                ? request.getTaxIdOrDni().trim()
                : "WEB-" + System.currentTimeMillis();

        Retailer retailer = retailerRepository.save(Retailer.builder()
                .firstName(firstName)
                .lastName(lastName)
                .documentType(DocumentType.DNI)
                .documentNumber(doc)
                .build());

        List<City> cities = cityRepository.findByIsDeletedFalse();
        String targetCity = request.getDeliveryCity() != null ? request.getDeliveryCity().trim() : "";
        City resolvedCity = cities.stream()
                .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(targetCity))
                .findFirst()
                .orElse(cities.isEmpty() ? null : cities.get(0));

        Address address = addressRepository.save(Address.builder()
                .street(request.getDeliveryAddress().trim())
                .neighborhood(request.getDeliveryNeighborhood() != null && !request.getDeliveryNeighborhood().isBlank()
                        ? request.getDeliveryNeighborhood().trim() : "Centro")
                .reference(request.getNotes())
                .latitude(-32.8895)
                .longitude(-68.8458)
                .city(resolvedCity)
                .build());

        String storeLabel = (request.getStoreName() != null && !request.getStoreName().isBlank())
                ? request.getStoreName().trim()
                : fullName + " (Web)";

        return pointOfSaleRepository.save(PointOfSale.builder()
                .storeName(storeLabel)
                .retailer(retailer)
                .address(address)
                .build());
    }
}

