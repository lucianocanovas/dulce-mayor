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
public class ProductRequestDTO {
    private String code;
    private String name;
    private String description;
    private String packaging;
    private int unitsPerPack;
    private boolean onSale;
    private String subCategoryId;
    private double price;
    private int initialStock;
    private String imageUrl;
}
