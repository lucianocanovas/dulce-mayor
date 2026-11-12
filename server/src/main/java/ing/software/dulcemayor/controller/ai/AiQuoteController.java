package ing.software.dulcemayor.controller.ai;

import ing.software.dulcemayor.dto.ai.AiChatRequestDTO;
import ing.software.dulcemayor.dto.ai.AiChatResponseDTO;
import ing.software.dulcemayor.dto.response.ApiResponse;
import ing.software.dulcemayor.service.ai.AiQuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the landing page AI wholesale quote & customer assistant chatbot.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiQuoteController {

    private final AiQuoteService aiQuoteService;

    @PostMapping("/quote")
    public ResponseEntity<ApiResponse<AiChatResponseDTO>> quote(@RequestBody AiChatRequestDTO request) {
        AiChatResponseDTO response = aiQuoteService.chat(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
