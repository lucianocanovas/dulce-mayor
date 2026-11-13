package ing.software.dulcemayor.controller;

import ing.software.dulcemayor.controller.base.BaseController;
import ing.software.dulcemayor.dto.actor.ClientSummaryDTO;
import ing.software.dulcemayor.dto.actor.CreateUserRequestDTO;
import ing.software.dulcemayor.dto.actor.UserResponseDTO;
import ing.software.dulcemayor.dto.response.ApiResponse;
import ing.software.dulcemayor.service.actor.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController extends BaseController<CreateUserRequestDTO, UserResponseDTO> {

    private final UserService userService;

    public UserController(UserService userService) {
        super(userService);
        this.userService = userService;
    }

    @GetMapping("/clients")
    public ResponseEntity<ApiResponse<List<ClientSummaryDTO>>> getAllClients() {
        return ResponseEntity.ok(ApiResponse.ok(userService.findAllClients()));
    }

    @Override
    protected String getCreatedMessage() {
        return "Personal registrado con clave provisoria 'dulcemayor'";
    }

    @Override
    protected String getUpdatedMessage() {
        return "Datos del usuario actualizados correctamente";
    }

    @Override
    protected String getDeletedMessage() {
        return "Usuario dado de baja";
    }
}
