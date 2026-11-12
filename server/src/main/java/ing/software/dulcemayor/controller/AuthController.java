package ing.software.dulcemayor.controller;

import ing.software.dulcemayor.dto.auth.AuthResponseDTO;
import ing.software.dulcemayor.dto.auth.ChangePasswordRequestDTO;
import ing.software.dulcemayor.dto.auth.LoginRequestDTO;
import ing.software.dulcemayor.dto.response.ApiResponse;
import ing.software.dulcemayor.service.actor.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Controlador independiente de autenticación (no extiende BaseController porque no es un CRUD estándar).
// Recibe DTOs de entrada, delega en AuthService y devuelve la respuesta envuelta en ApiResponse.
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Autenticación local con usuario y contraseña; devuelve token y datos del usuario
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Authentication successful", response));
    }

    // Autenticación social (Auth0 / Google / Facebook); registra o recupera al usuario
    @PostMapping("/social")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> socialLogin(@RequestBody ing.software.dulcemayor.dto.auth.SocialLoginRequestDTO request) {
        AuthResponseDTO response = authService.socialLogin(request);
        return ResponseEntity.ok(ApiResponse.ok("Social authentication successful", response));
    }

    // Cambia la contraseña del usuario (obligatorio en el primer ingreso con clave temporal)
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestParam String userId,
            @RequestBody ChangePasswordRequestDTO request) {
        authService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }
}
