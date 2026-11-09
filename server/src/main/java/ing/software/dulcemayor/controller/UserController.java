package ing.software.dulcemayor.controller;

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

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponseDTO>> create(@RequestBody CreateUserRequestDTO request) {
        UserResponseDTO created = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Personal registrado con clave provisoria 'dulcemayor'", created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Usuario dado de baja", null));
    }
}
