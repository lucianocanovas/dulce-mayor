package ing.software.dulcemayor.landing.dto;

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
public class ProductDTO {
    private String id;
    private String code;
    private String name;
    private String description;
    private String packaging;
    private int unitsPerPack;
    private boolean onSale;
    private String categoryName;
    private String subCategoryName;
    private double currentPrice;
    private int currentStock;
    private String imageUrl;
}
