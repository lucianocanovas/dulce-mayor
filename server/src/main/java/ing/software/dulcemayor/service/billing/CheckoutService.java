package ing.software.dulcemayor.service.billing;

import ing.software.dulcemayor.dto.billing.CheckoutRequestDTO;
import ing.software.dulcemayor.dto.billing.CheckoutResponseDTO;
import ing.software.dulcemayor.dto.order.OrderResponseDTO;
import ing.software.dulcemayor.entity.actor.Retailer;
import ing.software.dulcemayor.entity.actor.User;
import ing.software.dulcemayor.entity.location.Address;
import ing.software.dulcemayor.entity.location.City;
import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.entity.order.PointOfSale;
import ing.software.dulcemayor.enums.DocumentType;
import ing.software.dulcemayor.enums.PaymentType;
import ing.software.dulcemayor.repository.actor.RetailerRepository;
import ing.software.dulcemayor.repository.actor.UserRepository;
import ing.software.dulcemayor.repository.location.AddressRepository;
import ing.software.dulcemayor.repository.location.CityRepository;
import ing.software.dulcemayor.repository.order.OrderRepository;
import ing.software.dulcemayor.repository.order.PointOfSaleRepository;
import ing.software.dulcemayor.service.billing.strategy.PaymentInitiationResult;
import ing.software.dulcemayor.service.order.OrderService;
import ing.software.dulcemayor.util.InputValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles customer web checkouts originating from the Landing Storefront (:8081).
 */
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final RetailerRepository retailerRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CityRepository cityRepository;
    private final PointOfSaleRepository pointOfSaleRepository;

    @Transactional
    public CheckoutResponseDTO processLandingCheckout(CheckoutRequestDTO request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("El carrito de compras no puede estar vacío.");
        }

        PointOfSale selectedPos = resolveSelectedPointOfSale(request);
        if (selectedPos != null) {
            hydrateRequestFromPointOfSale(request, selectedPos);
        }

        if (request.getDeliveryCity() == null || request.getDeliveryCity().isBlank()) {
            request.setDeliveryCity("Mendoza Capital");
        }

        InputValidator.validateCheckoutRequest(request);

        PointOfSale pos = (selectedPos != null) ? selectedPos : createOrResolveWebPointOfSale(request);

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
        return findCustomerOrders(email, orderNumber, null);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> findCustomerOrders(String email, Long orderNumber, String userId) {
        if (userId != null && !userId.isBlank()) {
            User user = userRepository.findByIdAndIsDeletedFalse(userId).orElse(null);
            if (user != null) {
                Map<String, Order> merged = new LinkedHashMap<>();
                Retailer retailer = resolveRetailer(user);
                if (retailer != null) {
                    orderRepository.findByPointOfSaleRetailerIdAndIsDeletedFalseOrderByOrderDateTimeDesc(retailer.getId())
                            .forEach(o -> merged.put(o.getId(), o));
                }
                if (user.getUsername() != null && !user.getUsername().isBlank()) {
                    orderRepository.findByCustomerEmailIgnoreCaseAndIsDeletedFalseOrderByOrderDateTimeDesc(user.getUsername())
                            .forEach(o -> merged.putIfAbsent(o.getId(), o));
                }
                if (orderNumber != null) {
                    return merged.values().stream()
                            .filter(o -> orderNumber.equals(o.getOrderNumber()))
                            .sorted((a, b) -> b.getOrderDateTime().compareTo(a.getOrderDateTime()))
                            .map(orderService::toResponseDto)
                            .toList();
                }
                return merged.values().stream()
                        .sorted((a, b) -> b.getOrderDateTime().compareTo(a.getOrderDateTime()))
                        .map(orderService::toResponseDto)
                        .toList();
            }
        }
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

    private PointOfSale resolveSelectedPointOfSale(CheckoutRequestDTO request) {
        if (request == null || request.getPointOfSaleId() == null || request.getPointOfSaleId().isBlank()) {
            return null;
        }
        Retailer existingRetailer = null;
        if (request.getUserId() != null && !request.getUserId().isBlank()) {
            User user = userRepository.findByIdAndIsDeletedFalse(request.getUserId()).orElse(null);
            existingRetailer = resolveRetailer(user);
        }
        PointOfSale selected = pointOfSaleRepository.findByIdAndIsDeletedFalse(request.getPointOfSaleId().trim())
                .orElse(null);
        if (selected != null && (existingRetailer == null
                || (selected.getRetailer() != null && selected.getRetailer().getId().equals(existingRetailer.getId())))) {
            return selected;
        }
        return null;
    }

    private PointOfSale createOrResolveWebPointOfSale(CheckoutRequestDTO request) {
        Retailer existingRetailer = null;
        if (request.getUserId() != null && !request.getUserId().isBlank()) {
            User user = userRepository.findByIdAndIsDeletedFalse(request.getUserId()).orElse(null);
            existingRetailer = resolveRetailer(user);
        }

        if (request.getPointOfSaleId() != null && !request.getPointOfSaleId().isBlank()) {
            PointOfSale selected = pointOfSaleRepository.findByIdAndIsDeletedFalse(request.getPointOfSaleId().trim())
                    .orElse(null);
            if (selected != null && (existingRetailer == null
                    || (selected.getRetailer() != null && selected.getRetailer().getId().equals(existingRetailer.getId())))) {
                return selected;
            }
        }

        if (existingRetailer != null) {
            List<PointOfSale> existingPosList = pointOfSaleRepository.findByRetailerIdAndIsDeletedFalse(existingRetailer.getId());
            String reqAddress = request.getDeliveryAddress() != null ? request.getDeliveryAddress().trim() : "";
            for (PointOfSale pos : existingPosList) {
                if (pos.getAddress() != null && pos.getAddress().getStreet() != null
                        && pos.getAddress().getStreet().equalsIgnoreCase(reqAddress)) {
                    return pos;
                }
            }
        }

        String fullName = request.getCustomerName().trim();
        String[] parts = fullName.split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "Web";
        String doc = (request.getTaxIdOrDni() != null && !request.getTaxIdOrDni().isBlank())
                ? request.getTaxIdOrDni().trim()
                : "WEB-" + System.currentTimeMillis();

        Retailer retailer = (existingRetailer != null) ? existingRetailer : retailerRepository.save(Retailer.builder()
                .firstName(firstName)
                .lastName(lastName)
                .documentType(DocumentType.DNI)
                .documentNumber(doc)
                .phone(request.getCustomerPhone() != null ? request.getCustomerPhone().trim() : null)
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
                .contactName(fullName)
                .contactPhone(request.getCustomerPhone() != null ? request.getCustomerPhone().trim() : retailer.getPhone())
                .contactEmail(request.getCustomerEmail() != null ? request.getCustomerEmail().trim() : null)
                .taxIdOrDni(doc)
                .deliveryNotes(request.getNotes() != null ? request.getNotes().trim() : null)
                .retailer(retailer)
                .address(address)
                .build());
    }

    private void hydrateRequestFromPointOfSale(CheckoutRequestDTO request, PointOfSale pos) {
        if (pos == null) return;
        Retailer r = pos.getRetailer();
        Address a = pos.getAddress();

        if (request.getStoreName() == null || request.getStoreName().isBlank()) {
            request.setStoreName(pos.getStoreName());
        }
        if (request.getCustomerName() == null || request.getCustomerName().isBlank()) {
            if (pos.getContactName() != null && !pos.getContactName().isBlank()) {
                request.setCustomerName(pos.getContactName());
            } else if (r != null) {
                request.setCustomerName((r.getFirstName() + " " + r.getLastName()).trim());
            }
        }
        if (request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()) {
            if (pos.getContactPhone() != null && !pos.getContactPhone().isBlank()) {
                request.setCustomerPhone(pos.getContactPhone());
            } else if (r != null && r.getPhone() != null) {
                request.setCustomerPhone(r.getPhone());
            }
        }
        if (request.getCustomerEmail() == null || request.getCustomerEmail().isBlank()) {
            if (pos.getContactEmail() != null && !pos.getContactEmail().isBlank()) {
                request.setCustomerEmail(pos.getContactEmail());
            }
        }
        if (request.getTaxIdOrDni() == null || request.getTaxIdOrDni().isBlank()) {
            if (pos.getTaxIdOrDni() != null && !pos.getTaxIdOrDni().isBlank()) {
                request.setTaxIdOrDni(pos.getTaxIdOrDni());
            } else if (r != null && r.getDocumentNumber() != null) {
                request.setTaxIdOrDni(r.getDocumentNumber());
            }
        }
        if (a != null) {
            if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank()) {
                request.setDeliveryAddress(a.getStreet());
            }
            if (request.getDeliveryNeighborhood() == null || request.getDeliveryNeighborhood().isBlank()) {
                request.setDeliveryNeighborhood(a.getNeighborhood());
            }
            if ((request.getDeliveryCity() == null || request.getDeliveryCity().isBlank()) && a.getCity() != null) {
                request.setDeliveryCity(a.getCity().getName());
            }
        }
        if (request.getNotes() == null || request.getNotes().isBlank()) {
            if (pos.getDeliveryNotes() != null && !pos.getDeliveryNotes().isBlank()) {
                request.setNotes(pos.getDeliveryNotes());
            } else if (a != null && a.getReference() != null && !a.getReference().isBlank()) {
                request.setNotes(a.getReference());
            }
        }
    }

    private Retailer resolveRetailer(User user) {
        if (user == null || user.getPerson() == null || user.getPerson().getId() == null) {
            return null;
        }
        return retailerRepository.findByIdAndIsDeletedFalse(user.getPerson().getId()).orElse(null);
    }
}

