package ing.software.dulcemayor.service.actor;

import ing.software.dulcemayor.dto.actor.ClientSummaryDTO;
import ing.software.dulcemayor.dto.actor.CreateUserRequestDTO;
import ing.software.dulcemayor.dto.actor.UserResponseDTO;
import ing.software.dulcemayor.entity.actor.Employee;
import ing.software.dulcemayor.entity.actor.Person;
import ing.software.dulcemayor.entity.actor.Retailer;
import ing.software.dulcemayor.entity.actor.SalesRepresentative;
import ing.software.dulcemayor.entity.actor.User;
import ing.software.dulcemayor.entity.order.PointOfSale;
import ing.software.dulcemayor.enums.AuthProvider;
import ing.software.dulcemayor.enums.DocumentType;
import ing.software.dulcemayor.enums.UserRole;
import ing.software.dulcemayor.repository.actor.EmployeeRepository;
import ing.software.dulcemayor.repository.actor.RetailerRepository;
import ing.software.dulcemayor.repository.actor.SalesRepresentativeRepository;
import ing.software.dulcemayor.repository.actor.UserRepository;
import ing.software.dulcemayor.repository.order.PointOfSaleRepository;
import ing.software.dulcemayor.util.InputValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final SalesRepresentativeRepository salesRepRepository;
    private final RetailerRepository retailerRepository;
    private final PointOfSaleRepository pointOfSaleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return userRepository.findByIsDeletedFalse().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClientSummaryDTO> findAllClients() {
        List<Retailer> retailers = retailerRepository.findByIsDeletedFalse();
        List<User> retailerUsers = userRepository.findByIsDeletedFalse().stream()
                .filter(u -> u.getRole() == UserRole.RETAILER)
                .toList();

        Map<String, User> userByPersonId = new HashMap<>();
        List<User> orphanRetailerUsers = new ArrayList<>();
        for (User u : retailerUsers) {
            if (u.getPerson() != null) {
                userByPersonId.put(u.getPerson().getId(), u);
            } else {
                orphanRetailerUsers.add(u);
            }
        }

        List<ClientSummaryDTO> result = new ArrayList<>();
        for (Retailer r : retailers) {
            User webUser = userByPersonId.get(r.getId());
            List<PointOfSale> posList = pointOfSaleRepository.findByRetailerIdAndIsDeletedFalse(r.getId());
            PointOfSale primaryPos = posList.isEmpty() ? null : posList.get(0);

            String storeName = primaryPos != null ? primaryPos.getStoreName() : null;
            String address = (primaryPos != null && primaryPos.getAddress() != null)
                    ? primaryPos.getAddress().getStreet() : null;
            String neighborhood = (primaryPos != null && primaryPos.getAddress() != null)
                    ? primaryPos.getAddress().getNeighborhood() : null;
            String city = (primaryPos != null && primaryPos.getAddress() != null && primaryPos.getAddress().getCity() != null)
                    ? primaryPos.getAddress().getCity().getName() : null;
            String salesRepName = (primaryPos != null && primaryPos.getSalesRep() != null)
                    ? (primaryPos.getSalesRep().getFirstName() + " " + primaryPos.getSalesRep().getLastName()).trim()
                    : null;

            String fullName = (r.getFirstName() + " " + r.getLastName()).trim();

            result.add(ClientSummaryDTO.builder()
                    .retailerId(r.getId())
                    .userId(webUser != null ? webUser.getId() : null)
                    .username(webUser != null ? webUser.getUsername() : null)
                    .fullName(fullName)
                    .firstName(r.getFirstName())
                    .lastName(r.getLastName())
                    .documentNumber(r.getDocumentNumber())
                    .phone(r.getPhone())
                    .authProvider(webUser != null ? webUser.getAuthProvider() : null)
                    .hasWebAccount(webUser != null)
                    .storeName(storeName)
                    .address(address)
                    .neighborhood(neighborhood)
                    .city(city)
                    .salesRepName(salesRepName)
                    .pointsOfSaleCount(posList.size())
                    .active(webUser == null || !webUser.isDeleted())
                    .build());
        }

        for (User orphan : orphanRetailerUsers) {
            result.add(ClientSummaryDTO.builder()
                    .userId(orphan.getId())
                    .username(orphan.getUsername())
                    .fullName(orphan.getUsername())
                    .authProvider(orphan.getAuthProvider())
                    .hasWebAccount(true)
                    .pointsOfSaleCount(0)
                    .active(!orphan.isDeleted())
                    .build());
        }

        return result;
    }

    @Transactional
    public UserResponseDTO create(CreateUserRequestDTO request) {
        InputValidator.validateStaffUserRequest(request, true);
        String normalizedUsername = request.getUsername().trim().toLowerCase();

        if (userRepository.existsByUsernameAndIsDeletedFalse(normalizedUsername)) {
            throw new IllegalArgumentException("El nombre de usuario '" + normalizedUsername + "' ya se encuentra registrado.");
        }

        String empNumber = (request.getEmployeeIdNumber() != null && !request.getEmployeeIdNumber().isBlank())
                ? request.getEmployeeIdNumber().trim().toUpperCase()
                : null;
        if (empNumber != null) {
            employeeRepository.findByEmployeeIdNumberAndIsDeletedFalse(empNumber)
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("El legajo '" + empNumber + "' ya está asignado a otro empleado.");
                    });
        }

        String cleanDni = request.getDocumentNumber().trim();

        Person person;
        if (request.getRole() == UserRole.PRESALE_AGENT) {
            person = salesRepRepository.save(SalesRepresentative.builder()
                    .firstName(request.getFirstName().trim())
                    .lastName(request.getLastName().trim())
                    .documentType(DocumentType.DNI)
                    .documentNumber(cleanDni)
                    .employeeIdNumber(empNumber)
                    .build());
        } else {
            person = employeeRepository.save(Employee.builder()
                    .firstName(request.getFirstName().trim())
                    .lastName(request.getLastName().trim())
                    .documentType(DocumentType.DNI)
                    .documentNumber(cleanDni)
                    .employeeIdNumber(empNumber)
                    .build());
        }

        User user = User.builder()
                .username(normalizedUsername)
                .password(passwordEncoder.encode("dulcemayor"))
                .role(request.getRole())
                .isTemporaryPassword(true)
                .authProvider(AuthProvider.LOCAL)
                .person(person)
                .build();

        return mapToDTO(userRepository.save(user));
    }

    @Transactional
    public UserResponseDTO update(String id, CreateUserRequestDTO request) {
        InputValidator.validateStaffUserRequest(request, false);
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (user.getPerson() != null) {
            user.getPerson().setFirstName(request.getFirstName().trim());
            user.getPerson().setLastName(request.getLastName().trim());
            if (request.getDocumentNumber() != null && !request.getDocumentNumber().isBlank()) {
                user.getPerson().setDocumentNumber(request.getDocumentNumber().trim());
            }
            if (user.getPerson() instanceof Employee emp && request.getEmployeeIdNumber() != null) {
                String empNum = request.getEmployeeIdNumber().trim();
                emp.setEmployeeIdNumber(empNum.isEmpty() ? null : empNum.toUpperCase());
            }
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
            user.setTemporaryPassword(false);
        }

        return mapToDTO(userRepository.save(user));
    }

    @Transactional
    public void delete(String id) {
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new IllegalArgumentException("La cuenta principal de administración (@admin) está protegida y no puede darse de baja.");
        }
        user.setDeleted(true);
        userRepository.save(user);
    }

    private UserResponseDTO mapToDTO(User user) {
        Person person = user.getPerson();
        String firstName = person != null ? person.getFirstName() : null;
        String lastName = person != null ? person.getLastName() : null;
        String fullName = (person != null && firstName != null)
                ? (firstName + " " + (lastName != null ? lastName : "")).trim()
                : user.getUsername();
        String documentNumber = person != null ? person.getDocumentNumber() : null;
        String employeeIdNumber = (person instanceof Employee emp) ? emp.getEmployeeIdNumber() : null;

        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(fullName)
                .firstName(firstName)
                .lastName(lastName)
                .documentNumber(documentNumber)
                .employeeIdNumber(employeeIdNumber)
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .temporaryPassword(user.isTemporaryPassword())
                .active(!user.isDeleted())
                .build();
    }
}
