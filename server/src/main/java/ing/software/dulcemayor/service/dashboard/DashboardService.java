package ing.software.dulcemayor.service.dashboard;

import ing.software.dulcemayor.dto.dashboard.DashboardStatsDTO;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.repository.order.OrderRepository;
import ing.software.dulcemayor.repository.catalog.ProductRepository;
import ing.software.dulcemayor.repository.catalog.StockRepository;
import ing.software.dulcemayor.service.catalog.ProductService;
import ing.software.dulcemayor.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final OrderService orderService;

    private static final int LOW_STOCK_THRESHOLD = 20;

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats() {
        long totalProducts = productRepository.count();
        long lowStockCount = stockRepository.findLowStock(LOW_STOCK_THRESHOLD).size();
        long pendingOrdersCount = orderRepository.countByStatusAndIsDeletedFalse(OrderStatus.LOADED);
        double totalSales = orderRepository.sumTotalSales();

        var recentOrders = orderService.getAllOrders().stream()
                .sorted((a, b) -> b.getOrderDateTime().compareTo(a.getOrderDateTime()))
                .limit(5)
                .toList();

        var lowStockProducts = productService.getLowStockProducts(LOW_STOCK_THRESHOLD);

        return DashboardStatsDTO.builder()
                .totalProducts(totalProducts)
                .lowStockCount(lowStockCount)
                .pendingOrdersCount(pendingOrdersCount)
                .totalRevenue(totalSales)
                .recentOrders(recentOrders)
                .lowStockProducts(lowStockProducts)
                .build();
    }
}
