package ing.software.dulcemayor.service.actor;

import ing.software.dulcemayor.dto.actor.ClientSummaryDTO;
import ing.software.dulcemayor.dto.actor.CreateUserRequestDTO;
import ing.software.dulcemayor.dto.actor.UserResponseDTO;
import ing.software.dulcemayor.entity.actor.Employee;
import ing.software.dulcemayor.entity.actor.Person;
import ing.software.dulcemayor.entity.actor.Retailer;
import ing.software.dulcemayor.entity.actor.User;
import ing.software.dulcemayor.entity.order.PointOfSale;
import ing.software.dulcemayor.enums.AuthProvider;
import ing.software.dulcemayor.enums.UserRole;
import ing.software.dulcemayor.repository.actor.EmployeeRepository;
import ing.software.dulcemayor.repository.actor.RetailerRepository;
import ing.software.dulcemayor.repository.actor.UserRepository;
import ing.software.dulcemayor.repository.order.PointOfSaleRepository;
import ing.software.dulcemayor.service.actor.factory.PersonFactory;
import ing.software.dulcemayor.service.base.BaseService;
import ing.software.dulcemayor.util.InputValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService extends BaseService<User, CreateUserRequestDTO, UserResponseDTO> {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final RetailerRepository retailerRepository;
    private final PointOfSaleRepository pointOfSaleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PersonFactory personFactory;

    public UserService(UserRepository userRepository,
                       EmployeeRepository employeeRepository,
                       RetailerRepository retailerRepository,
                       PointOfSaleRepository pointOfSaleRepository,
                       PasswordEncoder passwordEncoder,
                       PersonFactory personFactory) {
        super(userRepository);
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.retailerRepository = retailerRepository;
        this.pointOfSaleRepository = pointOfSaleRepository;
        this.passwordEncoder = passwordEncoder;
        this.personFactory = personFactory;
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return getAll();
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

    @Override
    protected void validate(CreateUserRequestDTO request, String id) {
        boolean isCreate = (id == null);
        InputValidator.validateStaffUserRequest(request, isCreate);

        if (isCreate) {
            String normalizedUsername = request.getUsername().trim().toLowerCase();
            if (userRepository.existsByUsernameAndIsDeletedFalse(normalizedUsername)) {
                throw new IllegalArgumentException("El nombre de usuario '" + normalizedUsername + "' ya se encuentra registrado.");
            }
        }

        String empNumber = (request.getEmployeeIdNumber() != null && !request.getEmployeeIdNumber().isBlank())
                ? request.getEmployeeIdNumber().trim().toUpperCase()
                : null;
        if (empNumber != null) {
            employeeRepository.findByEmployeeIdNumberAndIsDeletedFalse(empNumber)
                    .ifPresent(existing -> {
                        if (isCreate) {
                            throw new IllegalArgumentException("El legajo '" + empNumber + "' ya está asignado a otro empleado.");
                        }
                        User targetUser = userRepository.findByIdAndIsDeletedFalse(id).orElse(null);
                        if (targetUser == null || targetUser.getPerson() == null || !existing.getId().equals(targetUser.getPerson().getId())) {
                            throw new IllegalArgumentException("El legajo '" + empNumber + "' ya está asignado a otro empleado.");
                        }
                    });
        }
    }

    @Override
    protected User toEntity(CreateUserRequestDTO request) {
        String normalizedUsername = request.getUsername().trim().toLowerCase();
        String empNumber = (request.getEmployeeIdNumber() != null && !request.getEmployeeIdNumber().isBlank())
                ? request.getEmployeeIdNumber().trim().toUpperCase()
                : null;

        Person person = personFactory.createAndSaveStaffByRole(
                request.getRole(),
                request.getFirstName(),
                request.getLastName(),
                request.getDocumentNumber(),
                empNumber
        );

        String rawPassword = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword().trim()
                : "dulcemayor";
        boolean isTemp = (request.getPassword() == null || request.getPassword().isBlank())
                || "dulcemayor".equalsIgnoreCase(rawPassword);

        return User.builder()
                .username(normalizedUsername)
                .password(passwordEncoder.encode(rawPassword))
                .role(request.getRole())
                .isTemporaryPassword(isTemp)
                .authProvider(AuthProvider.LOCAL)
                .person(person)
                .build();
    }

    @Override
    protected void updateEntityFromDto(User user, CreateUserRequestDTO request) {
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
    }

    @Override
    protected void validateDelete(User user) {
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new IllegalArgumentException("La cuenta principal de administración (@admin) está protegida y no puede darse de baja.");
        }
    }

    @Override
    protected void beforeDelete(User user) {
        if (user.getPerson() != null) {
            user.getPerson().setDeleted(true);
        }
    }

    @Override
    protected UserResponseDTO toResponseDto(User user) {
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
