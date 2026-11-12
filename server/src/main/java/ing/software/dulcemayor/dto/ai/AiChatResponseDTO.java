package ing.software.dulcemayor.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponseDTO {
    private String reply;
    private List<AiRecommendedItemDTO> recommendedItems;
    private double estimatedTotal;
    private Double estimatedTotalUsd;
    private Double usdRate;
    private List<String> activePromotions;
    private String disclaimer;
    private boolean authenticated;
    private boolean aiPowered;
}
