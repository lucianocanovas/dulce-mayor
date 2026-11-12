package ing.software.dulcemayor.controller;

import ing.software.dulcemayor.dto.actor.ClientSummaryDTO;
import ing.software.dulcemayor.dto.actor.CreateUserRequestDTO;
import ing.software.dulcemayor.dto.actor.UserResponseDTO;
import ing.software.dulcemayor.dto.response.ApiResponse;
import ing.software.dulcemayor.service.actor.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(userService.findAll()));
    }

    @GetMapping("/clients")
    public ResponseEntity<ApiResponse<List<ClientSummaryDTO>>> getAllClients() {
        return ResponseEntity.ok(ApiResponse.ok(userService.findAllClients()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponseDTO>> create(@RequestBody CreateUserRequestDTO request) {
        UserResponseDTO created = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Personal registrado con clave provisoria 'dulcemayor'", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> update(@PathVariable String id, @RequestBody CreateUserRequestDTO request) {
        UserResponseDTO updated = userService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Datos del usuario actualizados correctamente", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Usuario dado de baja", null));
    }
}
