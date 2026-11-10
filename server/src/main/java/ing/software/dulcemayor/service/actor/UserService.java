package ing.software.dulcemayor.service.actor;

import ing.software.dulcemayor.dto.actor.CreateUserRequestDTO;
import ing.software.dulcemayor.dto.actor.UserResponseDTO;
import ing.software.dulcemayor.entity.actor.Employee;
import ing.software.dulcemayor.entity.actor.Person;
import ing.software.dulcemayor.entity.actor.SalesRepresentative;
import ing.software.dulcemayor.entity.actor.User;
import ing.software.dulcemayor.enums.AuthProvider;
import ing.software.dulcemayor.enums.DocumentType;
import ing.software.dulcemayor.enums.UserRole;
import ing.software.dulcemayor.repository.actor.EmployeeRepository;
import ing.software.dulcemayor.repository.actor.SalesRepresentativeRepository;
import ing.software.dulcemayor.repository.actor.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final SalesRepresentativeRepository salesRepRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return userRepository.findByIsDeletedFalse().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponseDTO create(CreateUserRequestDTO request) {
        if (userRepository.existsByUsernameAndIsDeletedFalse(request.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario '" + request.getUsername() + "' ya existe.");
        }

        Person person;
        if (request.getRole() == UserRole.PRESALE_AGENT) {
            person = salesRepRepository.save(SalesRepresentative.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .documentType(DocumentType.DNI)
                    .documentNumber(request.getDocumentNumber())
                    .employeeIdNumber(request.getEmployeeIdNumber())
                    .build());
        } else {
            person = employeeRepository.save(Employee.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .documentType(DocumentType.DNI)
                    .documentNumber(request.getDocumentNumber())
                    .employeeIdNumber(request.getEmployeeIdNumber())
                    .build());
        }

        User user = User.builder()
                .username(request.getUsername())
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
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (user.getPerson() != null) {
            if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
                user.getPerson().setFirstName(request.getFirstName().trim());
            }
            if (request.getLastName() != null && !request.getLastName().isBlank()) {
                user.getPerson().setLastName(request.getLastName().trim());
            }
            if (request.getDocumentNumber() != null && !request.getDocumentNumber().isBlank()) {
                user.getPerson().setDocumentNumber(request.getDocumentNumber().trim());
            }
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setTemporaryPassword(false);
        }

        return mapToDTO(userRepository.save(user));
    }

    @Transactional
    public void delete(String id) {
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        user.setDeleted(true);
        userRepository.save(user);
    }

    private UserResponseDTO mapToDTO(User user) {
        String fullName = (user.getPerson() != null)
                ? user.getPerson().getFirstName() + " " + user.getPerson().getLastName()
                : user.getUsername();

        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(fullName)
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .temporaryPassword(user.isTemporaryPassword())
                .active(!user.isDeleted())
                .build();
    }
}
