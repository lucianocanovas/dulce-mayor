package ing.software.dulcemayor.landing.controller;

import ing.software.dulcemayor.landing.client.BackendApiClient;
import ing.software.dulcemayor.landing.dto.AiChatRequestDTO;
import ing.software.dulcemayor.landing.dto.AiChatResponseDTO;
import ing.software.dulcemayor.landing.dto.CustomerProfileDTO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ChatProxyController {

    private final BackendApiClient backendApiClient;

    @PostMapping("/api/landing/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody AiChatRequestDTO request,
            HttpSession session) {
        try {
            Object attr = session.getAttribute(GlobalModelAttributes.SESSION_CUSTOMER_KEY);
            if (attr instanceof CustomerProfileDTO customer) {
                request.setUserId(customer.getUserId());
            } else {
                request.setUserId(null);
            }
            AiChatResponseDTO response = backendApiClient.sendAiChat(request);
            return ResponseEntity.ok(Map.of("success", true, "data", response));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage() != null ? ex.getMessage() : "No se pudo consultar al asistente IA."
            ));
        }
    }
}
