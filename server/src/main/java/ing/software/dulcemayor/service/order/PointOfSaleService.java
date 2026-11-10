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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointOfSaleService {

    private final PointOfSaleRepository pointOfSaleRepository;
    private final RetailerRepository retailerRepository;
    private final AddressRepository addressRepository;
    private final CityRepository cityRepository;
    private final SalesRepresentativeRepository salesRepRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<PointOfSaleResponseDTO> getAllPointsOfSale() {
        return pointOfSaleRepository.findByIsDeletedFalse().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PointOfSaleResponseDTO getById(String id) {
        PointOfSale pos = pointOfSaleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Point of Sale not found with ID: " + id));
        return toResponseDTO(pos);
    }

    @Transactional
    public PointOfSaleResponseDTO createPointOfSale(CreatePointOfSaleRequestDTO request) {
        // Resolve or create retailer
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
                .documentNumber(request.getTaxIdOrDni() != null ? request.getTaxIdOrDni() : "00000000")
                .build());

        // Resolve City
        City city = cityRepository.findAll().stream().findFirst().orElse(null);

        // Save Address
        Address address = addressRepository.save(Address.builder()
                .street(request.getAddress() != null ? request.getAddress() : "Sin especificar")
                .streetNumber("")
                .neighborhood(request.getNeighborhood() != null ? request.getNeighborhood() : "Centro")
                .city(city)
                .latitude(request.getLatitude() != null ? request.getLatitude() : -32.8895)
                .longitude(request.getLongitude() != null ? request.getLongitude() : -68.8458)
                .build());

        // Resolve SalesRep
        SalesRepresentative rep = null;
        if (request.getSalesRepId() != null && !request.getSalesRepId().isBlank()) {
            rep = salesRepRepository.findByIdAndIsDeletedFalse(request.getSalesRepId()).orElse(null);
        }
        if (rep == null) {
            rep = salesRepRepository.findAll().stream().findFirst().orElse(null);
        }

        PointOfSale pos = PointOfSale.builder()
                .storeName(request.getStoreName())
                .retailer(retailer)
                .address(address)
                .salesRep(rep)
                .build();

        PointOfSale saved = pointOfSaleRepository.save(pos);
        return toResponseDTO(saved);
    }

    public PointOfSaleResponseDTO toResponseDTO(PointOfSale pos) {
        String ownerName = pos.getRetailer() != null
                ? pos.getRetailer().getFirstName() + " " + pos.getRetailer().getLastName()
                : "N/A";
        String taxId = pos.getRetailer() != null ? pos.getRetailer().getDocumentNumber() : "N/A";
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
                .anyMatch(o -> o.getStatus() == OrderStatus.LOADED || o.getStatus() == OrderStatus.INVOICED || o.getStatus() == OrderStatus.IN_DELIVERY);

        return PointOfSaleResponseDTO.builder()
                .id(pos.getId())
                .storeName(pos.getStoreName())
                .ownerName(ownerName.trim())
                .taxIdOrDni(taxId)
                .phone("261-42" + (pos.getId().hashCode() % 900000 + 100000))
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
