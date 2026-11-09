package ing.software.dulcemayor.service.actor;

import ing.software.dulcemayor.dto.auth.AuthResponseDTO;
import ing.software.dulcemayor.dto.auth.ChangePasswordRequestDTO;
import ing.software.dulcemayor.dto.auth.LoginRequestDTO;
import ing.software.dulcemayor.entity.actor.User;
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
