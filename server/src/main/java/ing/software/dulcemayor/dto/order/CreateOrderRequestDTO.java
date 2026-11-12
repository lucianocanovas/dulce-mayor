package ing.software.dulcemayor.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequestDTO {
    private String pointOfSaleId;
    private String salesRepId;
    private String couponCode;
    private String paymentType;
    private String sourceChannel;
    private String paymentNote;
    private List<CreateOrderItemDTO> items;
}
