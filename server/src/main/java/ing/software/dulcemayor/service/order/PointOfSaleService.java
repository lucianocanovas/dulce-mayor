package ing.software.dulcemayor.service.order;

import ing.software.dulcemayor.dto.order.CreatePointOfSaleRequestDTO;
import ing.software.dulcemayor.dto.order.PointOfSaleResponseDTO;
import ing.software.dulcemayor.entity.actor.Retailer;
import ing.software.dulcemayor.entity.actor.SalesRepresentative;
import ing.software.dulcemayor.entity.location.Address;
import ing.software.dulcemayor.entity.location.City;
import ing.software.dulcemayor.entity.order.PointOfSale;
import ing.software.dulcemayor.enums.DocumentType;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.repository.actor.RetailerRepository;
import ing.software.dulcemayor.repository.actor.SalesRepresentativeRepository;
import ing.software.dulcemayor.repository.location.AddressRepository;
import ing.software.dulcemayor.repository.location.CityRepository;
import ing.software.dulcemayor.repository.order.OrderRepository;
import ing.software.dulcemayor.repository.order.PointOfSaleRepository;
import ing.software.dulcemayor.service.base.BaseService;
import ing.software.dulcemayor.util.InputValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PointOfSaleService extends BaseService<PointOfSale, CreatePointOfSaleRequestDTO, PointOfSaleResponseDTO> {

    private final PointOfSaleRepository pointOfSaleRepository;
    private final RetailerRepository retailerRepository;
    private final AddressRepository addressRepository;
    private final CityRepository cityRepository;
    private final SalesRepresentativeRepository salesRepRepository;
    private final OrderRepository orderRepository;

    public PointOfSaleService(PointOfSaleRepository pointOfSaleRepository,
                              RetailerRepository retailerRepository,
                              AddressRepository addressRepository,
                              CityRepository cityRepository,
                              SalesRepresentativeRepository salesRepRepository,
                              OrderRepository orderRepository) {
        super(pointOfSaleRepository);
        this.pointOfSaleRepository = pointOfSaleRepository;
        this.retailerRepository = retailerRepository;
        this.addressRepository = addressRepository;
        this.cityRepository = cityRepository;
        this.salesRepRepository = salesRepRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<PointOfSaleResponseDTO> getAllPointsOfSale() {
        return getAll();
    }

    @Transactional
    public PointOfSaleResponseDTO createPointOfSale(CreatePointOfSaleRequestDTO request) {
        return create(request);
    }

    @Transactional
    public PointOfSaleResponseDTO updatePointOfSale(String id, CreatePointOfSaleRequestDTO request) {
        return update(id, request);
    }

    @Transactional
    public void deletePointOfSale(String id) {
        delete(id);
    }

    @Override
    protected void validate(CreatePointOfSaleRequestDTO request, String id) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos del local no pueden estar vacíos.");
        }
        if (request.getCity() == null || request.getCity().isBlank()) {
            request.setCity("Mendoza Capital");
        }
        if (id == null) {
            InputValidator.validatePointOfSaleRequest(request, false);
        }
    }

    @Override
    protected PointOfSale toEntity(CreatePointOfSaleRequestDTO request) {
        String ownerFullName = (request.getOwnerName() != null && !request.getOwnerName().isBlank())
                ? request.getOwnerName().trim()
                : "Responsable " + request.getStoreName();
        String[] nameParts = ownerFullName.split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        Retailer retailer = retailerRepository.save(Retailer.builder()
                .firstName(firstName)
                .lastName(lastName.isBlank() ? "Comercio" : lastName)
                .documentType(DocumentType.DNI)
                .documentNumber(request.getTaxIdOrDni() != null && !request.getTaxIdOrDni().isBlank()
                        ? request.getTaxIdOrDni().trim()
                        : "00000000")
                .phone(request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null)
                .build());

        City city = resolveCity(request.getCity());

        Address address = addressRepository.save(Address.builder()
                .street(request.getAddress() != null ? request.getAddress().trim() : "Sin especificar")
                .streetNumber("")
                .neighborhood(request.getNeighborhood() != null && !request.getNeighborhood().isBlank()
                        ? request.getNeighborhood().trim() : "Centro")
                .city(city)
                .reference(request.getDeliveryNotes() != null ? request.getDeliveryNotes().trim() : "")
                .latitude(request.getLatitude() != null ? request.getLatitude() : -32.8895)
                .longitude(request.getLongitude() != null ? request.getLongitude() : -68.8458)
                .build());

        SalesRepresentative rep = resolveSalesRep(request.getSalesRepId());

        return PointOfSale.builder()
                .storeName(request.getStoreName().trim())
                .contactName(ownerFullName)
                .contactPhone(request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null)
                .contactEmail(request.getContactEmail() != null && !request.getContactEmail().isBlank() ? request.getContactEmail().trim() : null)
                .taxIdOrDni(request.getTaxIdOrDni() != null && !request.getTaxIdOrDni().isBlank() ? request.getTaxIdOrDni().trim() : null)
                .deliveryNotes(request.getDeliveryNotes() != null && !request.getDeliveryNotes().isBlank() ? request.getDeliveryNotes().trim() : null)
                .retailer(retailer)
                .address(address)
                .salesRep(rep)
                .build();
    }

    @Override
    protected void updateEntityFromDto(PointOfSale pos, CreatePointOfSaleRequestDTO request) {
        if (request.getStoreName() != null && !request.getStoreName().isBlank()) {
            pos.setStoreName(request.getStoreName().trim());
        }
        if (request.getOwnerName() != null && !request.getOwnerName().isBlank()) {
            pos.setContactName(request.getOwnerName().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            pos.setContactPhone(request.getPhone().trim());
        }
        if (request.getContactEmail() != null) {
            pos.setContactEmail(request.getContactEmail().trim());
        }
        if (request.getTaxIdOrDni() != null && !request.getTaxIdOrDni().isBlank()) {
            pos.setTaxIdOrDni(request.getTaxIdOrDni().trim());
        }
        if (request.getDeliveryNotes() != null) {
            pos.setDeliveryNotes(request.getDeliveryNotes().trim());
        }

        if (pos.getRetailer() != null) {
            if (request.getOwnerName() != null && !request.getOwnerName().isBlank()) {
                String[] nameParts = request.getOwnerName().trim().split(" ", 2);
                pos.getRetailer().setFirstName(nameParts[0]);
                pos.getRetailer().setLastName(nameParts.length > 1 ? nameParts[1] : "");
            }
            if (request.getTaxIdOrDni() != null && !request.getTaxIdOrDni().isBlank()) {
                pos.getRetailer().setDocumentNumber(request.getTaxIdOrDni().trim());
            }
            if (request.getPhone() != null && !request.getPhone().isBlank()) {
                pos.getRetailer().setPhone(request.getPhone().trim());
            }
            retailerRepository.save(pos.getRetailer());
        }

        if (pos.getAddress() != null) {
            if (request.getAddress() != null && !request.getAddress().isBlank()) {
                pos.getAddress().setStreet(request.getAddress().trim());
            }
            if (request.getNeighborhood() != null && !request.getNeighborhood().isBlank()) {
                pos.getAddress().setNeighborhood(request.getNeighborhood().trim());
            }
            if (request.getCity() != null && !request.getCity().isBlank()) {
                pos.getAddress().setCity(resolveCity(request.getCity()));
            }
            if (request.getLatitude() != null) {
                pos.getAddress().setLatitude(request.getLatitude());
            }
            if (request.getLongitude() != null) {
                pos.getAddress().setLongitude(request.getLongitude());
            }
            if (request.getDeliveryNotes() != null) {
                pos.getAddress().setReference(request.getDeliveryNotes().trim());
            }
            addressRepository.save(pos.getAddress());
        }

        if (request.getSalesRepId() != null && !request.getSalesRepId().isBlank()) {
            salesRepRepository.findByIdAndIsDeletedFalse(request.getSalesRepId().trim())
                    .ifPresent(pos::setSalesRep);
        }
    }

    private City resolveCity(String targetCityName) {
        List<City> cities = cityRepository.findByIsDeletedFalse();
        if (cities.isEmpty()) {
            cities = cityRepository.findAll();
        }
        String target = targetCityName != null ? targetCityName.trim() : "";
        return cities.stream()
                .filter(c -> c.getName() != null && (c.getName().equalsIgnoreCase(target)
                        || (target.toLowerCase().contains("capital") && c.getName().equalsIgnoreCase("Capital"))))
                .findFirst()
                .orElse(cities.isEmpty() ? null : cities.get(0));
    }

    private SalesRepresentative resolveSalesRep(String salesRepId) {
        if (salesRepId != null && !salesRepId.isBlank()) {
            SalesRepresentative rep = salesRepRepository.findByIdAndIsDeletedFalse(salesRepId.trim()).orElse(null);
            if (rep != null) {
                return rep;
            }
        }
        return salesRepRepository.findByIsDeletedFalse().stream().findFirst().orElse(null);
    }

    @Override
    protected PointOfSaleResponseDTO toResponseDto(PointOfSale pos) {
        return toResponseDTO(pos);
    }

    public PointOfSaleResponseDTO toResponseDTO(PointOfSale pos) {
        String ownerName = (pos.getContactName() != null && !pos.getContactName().isBlank())
                ? pos.getContactName()
                : (pos.getRetailer() != null
                        ? pos.getRetailer().getFirstName() + " " + pos.getRetailer().getLastName()
                        : "N/A");
        String taxId = (pos.getTaxIdOrDni() != null && !pos.getTaxIdOrDni().isBlank())
                ? pos.getTaxIdOrDni()
                : (pos.getRetailer() != null ? pos.getRetailer().getDocumentNumber() : "N/A");
        String phone = (pos.getContactPhone() != null && !pos.getContactPhone().isBlank())
                ? pos.getContactPhone()
                : (pos.getRetailer() != null && pos.getRetailer().getPhone() != null && !pos.getRetailer().getPhone().isBlank()
                        ? pos.getRetailer().getPhone()
                        : "261-4200000");
        String contactEmail = (pos.getContactEmail() != null && !pos.getContactEmail().isBlank())
                ? pos.getContactEmail()
                : null;
        String deliveryNotes = (pos.getDeliveryNotes() != null && !pos.getDeliveryNotes().isBlank())
                ? pos.getDeliveryNotes()
                : (pos.getAddress() != null ? pos.getAddress().getReference() : null);

        String addressStr = pos.getAddress() != null ? pos.getAddress().getStreet() : "N/A";
        String neighborhood = pos.getAddress() != null ? pos.getAddress().getNeighborhood() : "Centro";
        String cityName = (pos.getAddress() != null && pos.getAddress().getCity() != null)
                ? pos.getAddress().getCity().getName()
                : "Mendoza Capital";
        double lat = (pos.getAddress() != null && pos.getAddress().getLatitude() != null)
                ? pos.getAddress().getLatitude()
                : -32.8895;
        double lng = (pos.getAddress() != null && pos.getAddress().getLongitude() != null)
                ? pos.getAddress().getLongitude()
                : -68.8458;

        String repId = pos.getSalesRep() != null ? pos.getSalesRep().getId() : null;
        String repName = pos.getSalesRep() != null
                ? pos.getSalesRep().getFirstName() + " " + pos.getSalesRep().getLastName()
                : "Sin asignar";

        boolean hasActiveOrder = orderRepository.findByPointOfSaleIdAndIsDeletedFalse(pos.getId()).stream()
                .anyMatch(o -> o.getStatus() == OrderStatus.PENDING_PAYMENT
                        || o.getStatus() == OrderStatus.LOADED
                        || o.getStatus() == OrderStatus.PAID
                        || o.getStatus() == OrderStatus.INVOICED
                        || o.getStatus() == OrderStatus.IN_DELIVERY);

        return PointOfSaleResponseDTO.builder()
                .id(pos.getId())
                .storeName(pos.getStoreName())
                .ownerName(ownerName.trim())
                .taxIdOrDni(taxId)
                .phone(phone)
                .contactEmail(contactEmail)
                .deliveryNotes(deliveryNotes)
                .address(addressStr)
                .neighborhood(neighborhood)
                .city(cityName)
                .latitude(lat)
                .longitude(lng)
                .salesRepId(repId)
                .salesRepName(repName)
                .retailerId(pos.getRetailer() != null ? pos.getRetailer().getId() : null)
                .hasActiveOrder(hasActiveOrder)
                .build();
    }
}
