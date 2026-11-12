package ing.software.dulcemayor.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendedItemDTO {
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
