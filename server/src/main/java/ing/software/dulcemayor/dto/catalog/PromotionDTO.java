package ing.software.dulcemayor.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDTO {
    private String id;
    private String code;
    private String description;
    private double discountPercentage;
    private Integer minimumQuantity;
    private LocalDate startDate;
    private LocalDate endDate;
    private String productId;
    private String productName;
    private boolean active;
}
