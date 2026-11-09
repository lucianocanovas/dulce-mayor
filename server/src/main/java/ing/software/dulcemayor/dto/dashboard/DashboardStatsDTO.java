package ing.software.dulcemayor.dto.dashboard;

import ing.software.dulcemayor.dto.catalog.ProductResponseDTO;
import ing.software.dulcemayor.dto.order.OrderResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {
    private long totalProducts;
    private long lowStockCount;
    private long pendingOrdersCount;
    private double totalRevenue;
    private List<OrderResponseDTO> recentOrders;
    private List<ProductResponseDTO> lowStockProducts;
}
