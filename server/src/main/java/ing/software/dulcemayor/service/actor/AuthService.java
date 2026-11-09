package ing.software.dulcemayor.service.actor;

import ing.software.dulcemayor.dto.auth.AuthResponseDTO;
import ing.software.dulcemayor.dto.auth.ChangePasswordRequestDTO;
import ing.software.dulcemayor.dto.auth.LoginRequestDTO;
import ing.software.dulcemayor.dto.auth.SocialLoginRequestDTO;
import ing.software.dulcemayor.entity.actor.Employee;
import ing.software.dulcemayor.entity.actor.User;
import ing.software.dulcemayor.enums.AuthProvider;
import ing.software.dulcemayor.enums.DocumentType;
import ing.software.dulcemayor.enums.UserRole;
import ing.software.dulcemayor.repository.actor.EmployeeRepository;
import ing.software.dulcemayor.repository.actor.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
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
        String identifier = (request.getEmail() != null && !request.getEmail().isBlank())
                ? request.getEmail()
                : request.getSub();

        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Missing email or sub identifier for social login");
        }

        User user = userRepository.findByUsernameAndIsDeletedFalse(identifier)
                .orElseGet(() -> {
                    String[] nameParts = (request.getName() != null && !request.getName().isBlank())
                            ? request.getName().split(" ", 2)
                            : new String[]{"Usuario", "Social"};

                    String firstName = nameParts[0];
                    String lastName = nameParts.length > 1 ? nameParts[1] : "";

                    Employee employee = employeeRepository.save(Employee.builder()
                            .firstName(firstName)
                            .lastName(lastName.isBlank() ? "Auth0" : lastName)
                            .documentType(DocumentType.DNI)
                            .documentNumber("SOC-" + UUID.randomUUID().toString().substring(0, 6))
                            .employeeIdNumber("EMP-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                            .build());

                    AuthProvider provider = request.getProvider() != null ? request.getProvider() : AuthProvider.GOOGLE;

                    return userRepository.save(User.builder()
                            .username(identifier)
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .role(UserRole.ADMINISTRATIVE)
                            .isTemporaryPassword(false)
                            .authProvider(provider)
                            .person(employee)
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
}
