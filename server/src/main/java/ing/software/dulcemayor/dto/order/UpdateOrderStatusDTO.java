package ing.software.dulcemayor.dto.order;

import ing.software.dulcemayor.enums.OrderStatus;
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
public class UpdateOrderStatusDTO {
    private OrderStatus newStatus;
    private String note;
}
