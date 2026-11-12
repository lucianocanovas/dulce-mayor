package ing.software.dulcemayor.landing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequestDTO {
    private String message;
    private String userId;
    private List<AiChatMessageDTO> history;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiChatMessageDTO {
        private String role;
        private String content;
    }
}
