package ing.software.dulcemayor.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDTO {
    private String id;
    private String code;
    private String name;
    private String description;
    private String packaging;
    private int unitsPerPack;
    private boolean onSale;
    private String subCategoryId;
    private String subCategoryName;
    private String categoryName;
    private double currentPrice;
    private int currentStock;
    private String imageBase64;
}
