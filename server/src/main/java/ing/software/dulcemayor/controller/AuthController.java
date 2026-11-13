package ing.software.dulcemayor.controller;

import ing.software.dulcemayor.dto.auth.AuthResponseDTO;
import ing.software.dulcemayor.dto.auth.ChangePasswordRequestDTO;
import ing.software.dulcemayor.dto.auth.CustomerProfileDTO;
import ing.software.dulcemayor.dto.auth.CustomerRegisterRequestDTO;
import ing.software.dulcemayor.dto.auth.LoginRequestDTO;
import ing.software.dulcemayor.dto.auth.UpdateCustomerProfileRequestDTO;
import ing.software.dulcemayor.dto.order.CreatePointOfSaleRequestDTO;
import ing.software.dulcemayor.dto.order.PointOfSaleResponseDTO;
import ing.software.dulcemayor.dto.response.ApiResponse;
import ing.software.dulcemayor.service.actor.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    // Registro de cliente minorista para el portal web (landing)
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> registerCustomer(
            @RequestBody CustomerRegisterRequestDTO request) {
        AuthResponseDTO response = authService.registerCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Customer registered successfully", response));
    }

    // Perfil del cliente autenticado (datos personales, comercio y dirección)
    @GetMapping("/customer/{userId}")
    public ResponseEntity<ApiResponse<CustomerProfileDTO>> getCustomerProfile(@PathVariable String userId) {
        CustomerProfileDTO profile = authService.getCustomerProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    // Actualiza datos del perfil del cliente autenticado
    @PutMapping("/customer/{userId}")
    public ResponseEntity<ApiResponse<CustomerProfileDTO>> updateCustomerProfile(
            @PathVariable String userId,
            @RequestBody UpdateCustomerProfileRequestDTO request) {
        CustomerProfileDTO profile = authService.updateCustomerProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Perfil actualizado correctamente", profile));
    }

    // Locales (puntos de venta) guardados del cliente
    @GetMapping("/customer/{userId}/stores")
    public ResponseEntity<ApiResponse<List<PointOfSaleResponseDTO>>> getCustomerStores(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(authService.getCustomerPointsOfSale(userId)));
    }

    // Agrega un nuevo local guardado para el cliente
    @PostMapping("/customer/{userId}/stores")
    public ResponseEntity<ApiResponse<PointOfSaleResponseDTO>> addCustomerStore(
            @PathVariable String userId,
            @RequestBody CreatePointOfSaleRequestDTO request) {
        PointOfSaleResponseDTO created = authService.addCustomerPointOfSale(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Local guardado con éxito", created));
    }
}
