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
        User user = userRepository.findByUsernameAndIsDeletedFalse(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
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
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password does not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTemporaryPassword(false);
        userRepository.save(user);
    }

    @Transactional
    public AuthResponseDTO registerCustomer(CustomerRegisterRequestDTO request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("El correo electrónico es obligatorio");
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByUsernameAndIsDeletedFalse(normalizedEmail)) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese correo electrónico");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres");
        }
        if (request.getFirstName() == null || request.getFirstName().isBlank()
                || request.getLastName() == null || request.getLastName().isBlank()) {
            throw new IllegalArgumentException("El nombre y apellido son obligatorios");
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
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

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

            if (user.getPerson() instanceof Retailer retailer) {
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
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (user.getPerson() != null) {
            if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
                user.getPerson().setFirstName(request.getFirstName().trim());
            }
            if (request.getLastName() != null && !request.getLastName().isBlank()) {
                user.getPerson().setLastName(request.getLastName().trim());
            }
            if (request.getTaxIdOrDni() != null && !request.getTaxIdOrDni().isBlank()) {
                user.getPerson().setDocumentNumber(request.getTaxIdOrDni().trim());
            }

            if (user.getPerson() instanceof Retailer retailer) {
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
            } else if (user.getPerson() instanceof Employee emp) {
                employeeRepository.save(emp);
            }
        }

        return getCustomerProfile(userId);
    }

    @Transactional(readOnly = true)
    public List<PointOfSaleResponseDTO> getCustomerPointsOfSale(String userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!(user.getPerson() instanceof Retailer retailer)) {
            return List.of();
        }
        return pointOfSaleRepository.findByRetailerIdAndIsDeletedFalse(retailer.getId())
                .stream()
                .map(pos -> toPosDto(pos, retailer))
                .toList();
    }

    @Transactional
    public PointOfSaleResponseDTO addCustomerPointOfSale(String userId, CreatePointOfSaleRequestDTO request) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (request == null || request.getStoreName() == null || request.getStoreName().isBlank()
                || request.getAddress() == null || request.getAddress().isBlank()) {
            throw new IllegalArgumentException("El nombre del local y la dirección son obligatorios");
        }

        Retailer retailer;
        if (user.getPerson() instanceof Retailer existing) {
            retailer = existing;
        } else {
            String first = user.getPerson() != null ? user.getPerson().getFirstName() : user.getUsername();
            String last = user.getPerson() != null ? user.getPerson().getLastName() : "Cliente";
            retailer = retailerRepository.save(Retailer.builder()
                    .firstName(first)
                    .lastName(last)
                    .documentType(DocumentType.DNI)
                    .documentNumber("CLI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .phone(request.getPhone())
                    .build());
        }

        Address address = addressRepository.save(Address.builder()
                .street(request.getAddress().trim())
                .neighborhood(request.getNeighborhood() != null && !request.getNeighborhood().isBlank()
                        ? request.getNeighborhood().trim() : "Centro")
                .latitude(request.getLatitude() != null && request.getLatitude() != 0 ? request.getLatitude() : -32.8895)
                .longitude(request.getLongitude() != null && request.getLongitude() != 0 ? request.getLongitude() : -68.8458)
                .city(resolveCity(request.getCity()))
                .build());

        PointOfSale saved = pointOfSaleRepository.save(PointOfSale.builder()
                .storeName(request.getStoreName().trim())
                .retailer(retailer)
                .address(address)
                .build());

        return toPosDto(saved, retailer);
    }

    private City resolveCity(String targetCityName) {
        List<City> cities = cityRepository.findByIsDeletedFalse();
        String target = targetCityName != null ? targetCityName.trim() : "";
        return cities.stream()
                .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(target))
                .findFirst()
                .orElse(cities.isEmpty() ? null : cities.get(0));
    }

    private PointOfSaleResponseDTO toPosDto(PointOfSale pos, Retailer retailer) {
        Address addr = pos.getAddress();
        return PointOfSaleResponseDTO.builder()
                .id(pos.getId())
                .storeName(pos.getStoreName())
                .ownerName((retailer.getFirstName() + " " + retailer.getLastName()).trim())
                .taxIdOrDni(retailer.getDocumentNumber())
                .phone(retailer.getPhone())
                .address(addr != null ? addr.getStreet() : "")
                .neighborhood(addr != null ? addr.getNeighborhood() : "")
                .city(addr != null && addr.getCity() != null ? addr.getCity().getName() : "Mendoza Capital")
                .latitude(addr != null ? addr.getLatitude() : -32.8895)
                .longitude(addr != null ? addr.getLongitude() : -68.8458)
                .retailerId(retailer.getId())
                .build();
    }
}
