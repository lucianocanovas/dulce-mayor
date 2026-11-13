package ing.software.dulcemayor.service.actor;

import ing.software.dulcemayor.dto.auth.AuthResponseDTO;
import ing.software.dulcemayor.dto.auth.ChangePasswordRequestDTO;
import ing.software.dulcemayor.dto.auth.CustomerProfileDTO;
import ing.software.dulcemayor.dto.auth.CustomerRegisterRequestDTO;
import ing.software.dulcemayor.dto.auth.LoginRequestDTO;
import ing.software.dulcemayor.dto.auth.SocialLoginRequestDTO;
import ing.software.dulcemayor.dto.auth.UpdateCustomerProfileRequestDTO;
import ing.software.dulcemayor.dto.order.CreatePointOfSaleRequestDTO;
import ing.software.dulcemayor.dto.order.PointOfSaleResponseDTO;
import ing.software.dulcemayor.entity.actor.Employee;
import ing.software.dulcemayor.entity.actor.Retailer;
import ing.software.dulcemayor.entity.actor.User;
import ing.software.dulcemayor.entity.location.Address;
import ing.software.dulcemayor.entity.location.City;
import ing.software.dulcemayor.entity.order.PointOfSale;
import ing.software.dulcemayor.enums.AuthProvider;
import ing.software.dulcemayor.enums.DocumentType;
import ing.software.dulcemayor.enums.UserRole;
import ing.software.dulcemayor.repository.actor.EmployeeRepository;
import ing.software.dulcemayor.repository.actor.RetailerRepository;
import ing.software.dulcemayor.repository.actor.UserRepository;
import ing.software.dulcemayor.repository.location.AddressRepository;
import ing.software.dulcemayor.repository.location.CityRepository;
import ing.software.dulcemayor.repository.order.PointOfSaleRepository;
import ing.software.dulcemayor.util.InputValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final RetailerRepository retailerRepository;
    private final AddressRepository addressRepository;
    private final CityRepository cityRepository;
    private final PointOfSaleRepository pointOfSaleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Debe ingresar su usuario y contraseña.");
        }

        User user = userRepository.findByUsernameAndIsDeletedFalse(request.getUsername().trim())
                .orElseThrow(() -> new IllegalArgumentException("Usuario o contraseña incorrectos"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Usuario o contraseña incorrectos");
        }

        String fullName = (user.getPerson() != null)
                ? user.getPerson().getFirstName() + " " + user.getPerson().getLastName()
                : user.getUsername();

        return AuthResponseDTO.builder()
                .token("jwt-" + UUID.randomUUID())
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(fullName)
                .role(user.getRole())
                .temporaryPassword(user.isTemporaryPassword())
                .build();
    }

    @Transactional
    public AuthResponseDTO socialLogin(SocialLoginRequestDTO request) {
        String rawIdentifier = (request.getEmail() != null && !request.getEmail().isBlank())
                ? request.getEmail().trim()
                : (request.getSub() != null ? request.getSub().trim() : null);

        if (rawIdentifier == null || rawIdentifier.isBlank()) {
            throw new IllegalArgumentException("Missing email or sub identifier for social login");
        }

        String identifier = rawIdentifier.contains("@") ? rawIdentifier.toLowerCase() : rawIdentifier;

        User user = userRepository.findByUsernameAndIsDeletedFalse(identifier)
                .orElseGet(() -> {
                    String[] nameParts = (request.getName() != null && !request.getName().isBlank())
                            ? request.getName().trim().split(" ", 2)
                            : new String[]{"Usuario", "Google"};

                    String firstName = nameParts[0];
                    String lastName = nameParts.length > 1 && !nameParts[1].isBlank() ? nameParts[1] : "Google";
                    AuthProvider provider = request.getProvider() != null ? request.getProvider() : AuthProvider.GOOGLE;
                    UserRole targetRole = request.getDefaultRole() != null ? request.getDefaultRole() : UserRole.ADMINISTRATIVE;

                    ing.software.dulcemayor.entity.actor.Person person;
                    if (targetRole == UserRole.RETAILER) {
                        person = retailerRepository.save(Retailer.builder()
                                .firstName(firstName)
                                .lastName(lastName)
                                .documentType(DocumentType.DNI)
                                .documentNumber("CLI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                                .build());
                    } else {
                        person = employeeRepository.save(Employee.builder()
                                .firstName(firstName)
                                .lastName(lastName)
                                .documentType(DocumentType.DNI)
                                .documentNumber("SOC-" + UUID.randomUUID().toString().substring(0, 6))
                                .employeeIdNumber("EMP-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                                .build());
                    }

                    return userRepository.save(User.builder()
                            .username(identifier)
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .role(targetRole)
                            .isTemporaryPassword(false)
                            .authProvider(provider)
                            .person(person)
                            .build());
                });

        String fullName = (user.getPerson() != null)
                ? user.getPerson().getFirstName() + " " + user.getPerson().getLastName()
                : user.getUsername();

        return AuthResponseDTO.builder()
                .token("jwt-" + UUID.randomUUID())
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(fullName)
                .role(user.getRole())
                .temporaryPassword(user.isTemporaryPassword())
                .build();
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequestDTO request) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Identificador de usuario obligatorio");
        }
        InputValidator.validateChangePassword(request);

        User user = userRepository.findByIdAndIsDeletedFalse(userId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no coincide");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        user.setTemporaryPassword(false);
        userRepository.save(user);
    }

    @Transactional
    public AuthResponseDTO registerCustomer(CustomerRegisterRequestDTO request) {
        InputValidator.validateCustomerRegistration(request);
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByUsernameAndIsDeletedFalse(normalizedEmail)) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese correo electrónico");
        }

        String docNumber = (request.getTaxIdOrDni() != null && !request.getTaxIdOrDni().isBlank())
                ? request.getTaxIdOrDni().trim()
                : "CLI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Retailer retailer = retailerRepository.save(Retailer.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .documentType(DocumentType.DNI)
                .documentNumber(docNumber)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .build());

        if (request.getDeliveryAddress() != null && !request.getDeliveryAddress().isBlank()) {
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
                    .latitude(-32.8895)
                    .longitude(-68.8458)
                    .city(resolvedCity)
                    .build());

            String storeLabel = (request.getStoreName() != null && !request.getStoreName().isBlank())
                    ? request.getStoreName().trim()
                    : retailer.getFirstName() + " " + retailer.getLastName();

            pointOfSaleRepository.save(PointOfSale.builder()
                    .storeName(storeLabel)
                    .retailer(retailer)
                    .address(address)
                    .build());
        }

        User user = userRepository.save(User.builder()
                .username(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.RETAILER)
                .isTemporaryPassword(false)
                .authProvider(AuthProvider.LOCAL)
                .person(retailer)
                .build());

        String fullName = retailer.getFirstName() + " " + retailer.getLastName();
        return AuthResponseDTO.builder()
                .token("jwt-" + UUID.randomUUID())
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(fullName)
                .role(user.getRole())
                .temporaryPassword(false)
                .build();
    }

    @Transactional(readOnly = true)
    public CustomerProfileDTO getCustomerProfile(String userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String firstName = "";
        String lastName = "";
        String fullName = user.getUsername();
        String phone = null;
        String taxIdOrDni = null;
        String retailerId = null;
        String storeName = null;
        String deliveryAddress = null;
        String deliveryNeighborhood = null;
        String deliveryCity = null;

        if (user.getPerson() != null) {
            firstName = user.getPerson().getFirstName();
            lastName = user.getPerson().getLastName();
            fullName = (firstName + " " + lastName).trim();
            taxIdOrDni = user.getPerson().getDocumentNumber();

            Retailer retailer = resolveRetailer(user);
            if (retailer != null) {
                retailerId = retailer.getId();
                phone = retailer.getPhone();
                List<PointOfSale> posList = pointOfSaleRepository.findByRetailerIdAndIsDeletedFalse(retailer.getId());
                if (!posList.isEmpty()) {
                    PointOfSale pos = posList.get(0);
                    storeName = pos.getStoreName();
                    if (pos.getAddress() != null) {
                        deliveryAddress = pos.getAddress().getStreet();
                        deliveryNeighborhood = pos.getAddress().getNeighborhood();
                        if (pos.getAddress().getCity() != null) {
                            deliveryCity = pos.getAddress().getCity().getName();
                        }
                    }
                }
            }
        }

        return CustomerProfileDTO.builder()
                .userId(user.getId())
                .retailerId(retailerId)
                .email(user.getUsername())
                .fullName(fullName)
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .storeName(storeName)
                .taxIdOrDni(taxIdOrDni)
                .deliveryAddress(deliveryAddress)
                .deliveryNeighborhood(deliveryNeighborhood)
                .deliveryCity(deliveryCity)
                .role(user.getRole())
                .build();
    }

    @Transactional
    public CustomerProfileDTO updateCustomerProfile(String userId, UpdateCustomerProfileRequestDTO request) {
        InputValidator.validateProfileUpdate(request);
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (user.getPerson() != null) {
            Retailer retailer = resolveRetailer(user);
            if (retailer != null) {
                if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
                    retailer.setFirstName(request.getFirstName().trim());
                }
                if (request.getLastName() != null && !request.getLastName().isBlank()) {
                    retailer.setLastName(request.getLastName().trim());
                }
                if (request.getTaxIdOrDni() != null && !request.getTaxIdOrDni().isBlank()) {
                    retailer.setDocumentNumber(request.getTaxIdOrDni().trim());
                }
                if (request.getPhone() != null) {
                    retailer.setPhone(request.getPhone().trim());
                }
                retailerRepository.save(retailer);

                boolean hasStoreData = (request.getStoreName() != null && !request.getStoreName().isBlank())
                        || (request.getDeliveryAddress() != null && !request.getDeliveryAddress().isBlank());
                if (hasStoreData) {
                    List<PointOfSale> posList = pointOfSaleRepository.findByRetailerIdAndIsDeletedFalse(retailer.getId());
                    if (!posList.isEmpty()) {
                        PointOfSale primary = posList.get(0);
                        if (request.getStoreName() != null && !request.getStoreName().isBlank()) {
                            primary.setStoreName(request.getStoreName().trim());
                        }
                        Address addr = primary.getAddress();
                        if (addr == null) {
                            addr = Address.builder().latitude(-32.8895).longitude(-68.8458).build();
                        }
                        if (request.getDeliveryAddress() != null && !request.getDeliveryAddress().isBlank()) {
                            addr.setStreet(request.getDeliveryAddress().trim());
                        }
                        if (request.getDeliveryNeighborhood() != null && !request.getDeliveryNeighborhood().isBlank()) {
                            addr.setNeighborhood(request.getDeliveryNeighborhood().trim());
                        }
                        if (request.getDeliveryCity() != null && !request.getDeliveryCity().isBlank()) {
                            addr.setCity(resolveCity(request.getDeliveryCity()));
                        }
                        primary.setAddress(addressRepository.save(addr));
                        pointOfSaleRepository.save(primary);
                    } else if (request.getDeliveryAddress() != null && !request.getDeliveryAddress().isBlank()) {
                        Address addr = addressRepository.save(Address.builder()
                                .street(request.getDeliveryAddress().trim())
                                .neighborhood(request.getDeliveryNeighborhood() != null && !request.getDeliveryNeighborhood().isBlank()
                                        ? request.getDeliveryNeighborhood().trim() : "Centro")
                                .latitude(-32.8895)
                                .longitude(-68.8458)
                                .city(resolveCity(request.getDeliveryCity()))
                                .build());
                        String storeLabel = (request.getStoreName() != null && !request.getStoreName().isBlank())
                                ? request.getStoreName().trim()
                                : (retailer.getFirstName() + " " + retailer.getLastName()).trim();
                        pointOfSaleRepository.save(PointOfSale.builder()
                                .storeName(storeLabel)
                                .retailer(retailer)
                                .address(addr)
                                .build());
                    }
                }
            } else {
                Employee emp = employeeRepository.findByIdAndIsDeletedFalse(user.getPerson().getId()).orElse(null);
                if (emp != null) {
                    if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
                        emp.setFirstName(request.getFirstName().trim());
                    }
                    if (request.getLastName() != null && !request.getLastName().isBlank()) {
                        emp.setLastName(request.getLastName().trim());
                    }
                    if (request.getTaxIdOrDni() != null && !request.getTaxIdOrDni().isBlank()) {
                        emp.setDocumentNumber(request.getTaxIdOrDni().trim());
                    }
                    employeeRepository.save(emp);
                }
            }
        }

        return getCustomerProfile(userId);
    }

    @Transactional(readOnly = true)
    public List<PointOfSaleResponseDTO> getCustomerPointsOfSale(String userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Retailer retailer = resolveRetailer(user);
        if (retailer == null) {
            return List.of();
        }
        return pointOfSaleRepository.findByRetailerIdAndIsDeletedFalse(retailer.getId())
                .stream()
                .map(pos -> toPosDto(pos, retailer, user))
                .toList();
    }

    @Transactional
    public PointOfSaleResponseDTO addCustomerPointOfSale(String userId, CreatePointOfSaleRequestDTO request) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (request == null) {
            throw new IllegalArgumentException("Los datos del local no pueden estar vacíos.");
        }
        if (request.getCity() == null || request.getCity().isBlank()) {
            request.setCity("Mendoza Capital");
        }

        Retailer existingRetailer = resolveRetailer(user);
        if ((request.getOwnerName() == null || request.getOwnerName().isBlank()) && existingRetailer != null) {
            request.setOwnerName((existingRetailer.getFirstName() + " " + existingRetailer.getLastName()).trim());
        }
        if ((request.getPhone() == null || request.getPhone().isBlank()) && existingRetailer != null && existingRetailer.getPhone() != null) {
            request.setPhone(existingRetailer.getPhone());
        }

        InputValidator.validatePointOfSaleRequest(request, true);

        Retailer retailer = existingRetailer;
        if (retailer == null) {
            String first = user.getPerson() != null ? user.getPerson().getFirstName() : user.getUsername();
            String last = user.getPerson() != null ? user.getPerson().getLastName() : "Cliente";
            retailer = retailerRepository.save(Retailer.builder()
                    .firstName(first)
                    .lastName(last)
                    .documentType(DocumentType.DNI)
                    .documentNumber(request.getTaxIdOrDni() != null && !request.getTaxIdOrDni().isBlank()
                            ? request.getTaxIdOrDni().trim()
                            : "CLI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .phone(request.getPhone())
                    .build());
        } else if (request.getPhone() != null && !request.getPhone().isBlank()
                && (retailer.getPhone() == null || retailer.getPhone().isBlank())) {
            retailer.setPhone(request.getPhone().trim());
            retailer = retailerRepository.save(retailer);
        }

        String defaultOwner = (retailer.getFirstName() + " " + retailer.getLastName()).trim();
        String contactName = (request.getOwnerName() != null && !request.getOwnerName().isBlank())
                ? request.getOwnerName().trim() : defaultOwner;
        String contactPhone = (request.getPhone() != null && !request.getPhone().isBlank())
                ? request.getPhone().trim() : retailer.getPhone();
        String contactEmail = (request.getContactEmail() != null && !request.getContactEmail().isBlank())
                ? request.getContactEmail().trim() : user.getUsername();
        String taxIdOrDni = (request.getTaxIdOrDni() != null && !request.getTaxIdOrDni().isBlank())
                ? request.getTaxIdOrDni().trim() : retailer.getDocumentNumber();
        String deliveryNotes = (request.getDeliveryNotes() != null && !request.getDeliveryNotes().isBlank())
                ? request.getDeliveryNotes().trim() : null;

        Address address = addressRepository.save(Address.builder()
                .street(request.getAddress().trim())
                .neighborhood(request.getNeighborhood() != null && !request.getNeighborhood().isBlank()
                        ? request.getNeighborhood().trim() : "Centro")
                .reference(deliveryNotes)
                .latitude(request.getLatitude() != null && request.getLatitude() != 0 ? request.getLatitude() : -32.8895)
                .longitude(request.getLongitude() != null && request.getLongitude() != 0 ? request.getLongitude() : -68.8458)
                .city(resolveCity(request.getCity()))
                .build());

        PointOfSale saved = pointOfSaleRepository.save(PointOfSale.builder()
                .storeName(request.getStoreName().trim())
                .contactName(contactName)
                .contactPhone(contactPhone)
                .contactEmail(contactEmail)
                .taxIdOrDni(taxIdOrDni)
                .deliveryNotes(deliveryNotes)
                .retailer(retailer)
                .address(address)
                .build());

        return toPosDto(saved, retailer, user);
    }

    @Transactional
    public PointOfSaleResponseDTO updateCustomerPointOfSale(String userId, String storeId, CreatePointOfSaleRequestDTO request) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Retailer retailer = resolveRetailer(user);
        if (retailer == null) {
            throw new IllegalArgumentException("Cliente no encontrado");
        }
        PointOfSale pos = pointOfSaleRepository.findByIdAndIsDeletedFalse(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Local no encontrado"));
        if (pos.getRetailer() == null || !retailer.getId().equals(pos.getRetailer().getId())) {
            throw new IllegalArgumentException("No tenés permisos para modificar este local");
        }

        if (request == null) {
            throw new IllegalArgumentException("Los datos del local no pueden estar vacíos.");
        }
        if (request.getCity() == null || request.getCity().isBlank()) {
            request.setCity("Mendoza Capital");
        }
        InputValidator.validatePointOfSaleRequest(request, true);

        pos.setStoreName(request.getStoreName().trim());
        pos.setContactName(request.getOwnerName().trim());
        pos.setContactPhone(request.getPhone().trim());
        pos.setContactEmail(request.getContactEmail() != null ? request.getContactEmail().trim() : "");
        pos.setTaxIdOrDni(request.getTaxIdOrDni() != null ? request.getTaxIdOrDni().trim() : "");
        pos.setDeliveryNotes(request.getDeliveryNotes() != null ? request.getDeliveryNotes().trim() : "");

        Address addr = pos.getAddress();
        if (addr == null) {
            addr = Address.builder().latitude(-32.8895).longitude(-68.8458).build();
        }
        addr.setStreet(request.getAddress().trim());
        addr.setNeighborhood(request.getNeighborhood() != null && !request.getNeighborhood().isBlank()
                ? request.getNeighborhood().trim() : "Centro");
        addr.setCity(resolveCity(request.getCity()));
        addr.setReference(request.getDeliveryNotes() != null ? request.getDeliveryNotes().trim() : "");
        pos.setAddress(addressRepository.save(addr));

        PointOfSale saved = pointOfSaleRepository.save(pos);
        return toPosDto(saved, retailer, user);
    }

    @Transactional
    public void deleteCustomerPointOfSale(String userId, String storeId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Retailer retailer = resolveRetailer(user);
        if (retailer == null) {
            throw new IllegalArgumentException("Cliente no encontrado");
        }
        PointOfSale pos = pointOfSaleRepository.findByIdAndIsDeletedFalse(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Local no encontrado"));
        if (pos.getRetailer() == null || !retailer.getId().equals(pos.getRetailer().getId())) {
            throw new IllegalArgumentException("No tenés permisos para eliminar este local");
        }
        pos.setDeleted(true);
        pointOfSaleRepository.save(pos);
    }

    private Retailer resolveRetailer(User user) {
        if (user == null || user.getPerson() == null) {
            return null;
        }
        if (user.getPerson().getId() != null) {
            Retailer persisted = retailerRepository.findByIdAndIsDeletedFalse(user.getPerson().getId()).orElse(null);
            if (persisted != null) {
                return persisted;
            }
        }
        if (user.getPerson() instanceof Retailer retailer && !retailer.isDeleted()) {
            return retailer;
        }
        return null;
    }

    private City resolveCity(String targetCityName) {
        List<City> cities = cityRepository.findByIsDeletedFalse();
        String target = targetCityName != null ? targetCityName.trim() : "";
        return cities.stream()
                .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(target))
                .findFirst()
                .orElse(cities.isEmpty() ? null : cities.get(0));
    }

    private PointOfSaleResponseDTO toPosDto(PointOfSale pos, Retailer retailer, User user) {
        Address addr = pos.getAddress();
        String fallbackOwner = (retailer.getFirstName() + " " + retailer.getLastName()).trim();
        String ownerName = (pos.getContactName() != null && !pos.getContactName().isBlank())
                ? pos.getContactName() : fallbackOwner;
        String phone = (pos.getContactPhone() != null && !pos.getContactPhone().isBlank())
                ? pos.getContactPhone() : retailer.getPhone();
        String contactEmail = (pos.getContactEmail() != null && !pos.getContactEmail().isBlank())
                ? pos.getContactEmail() : (user != null ? user.getUsername() : "");
        String taxIdOrDni = (pos.getTaxIdOrDni() != null && !pos.getTaxIdOrDni().isBlank())
                ? pos.getTaxIdOrDni() : retailer.getDocumentNumber();
        String deliveryNotes = (pos.getDeliveryNotes() != null && !pos.getDeliveryNotes().isBlank())
                ? pos.getDeliveryNotes() : (addr != null && addr.getReference() != null ? addr.getReference() : "");

        return PointOfSaleResponseDTO.builder()
                .id(pos.getId())
                .storeName(pos.getStoreName())
                .ownerName(ownerName)
                .taxIdOrDni(taxIdOrDni)
                .phone(phone)
                .contactEmail(contactEmail)
                .address(addr != null ? addr.getStreet() : "")
                .neighborhood(addr != null ? addr.getNeighborhood() : "")
                .city(addr != null && addr.getCity() != null ? addr.getCity().getName() : "Mendoza Capital")
                .deliveryNotes(deliveryNotes)
                .latitude(addr != null && addr.getLatitude() != null ? addr.getLatitude() : -32.8895)
                .longitude(addr != null && addr.getLongitude() != null ? addr.getLongitude() : -68.8458)
                .retailerId(retailer.getId())
                .build();
    }
}
