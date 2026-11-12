package ing.software.dulcemayor.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderItemDTO {
    private String productId;
    private String productCode;
    private int quantity;
}
