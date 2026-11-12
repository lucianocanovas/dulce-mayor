package ing.software.dulcemayor.landing.dto;

import lombok.Data;

import java.util.List;

@Data
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

    @Data
    public static class AiRecommendedItemDTO {
        private String productId;
        private String code;
        private String name;
        private String packaging;
        private int unitsPerPack;
        private int quantity;
        private double unitPrice;
        private double subtotal;
        private int currentStock;
    }
}
