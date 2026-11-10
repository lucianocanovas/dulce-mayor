package ing.software.dulcemayor.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponseDTO {
    private String id;
    private String productId;
    private String productCode;
    private String productName;
    private String packaging;
    private int quantity;
    private double unitPrice;
    private double subtotal;
}
