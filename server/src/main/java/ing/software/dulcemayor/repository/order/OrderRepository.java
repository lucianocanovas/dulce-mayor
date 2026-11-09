package ing.software.dulcemayor.repository.order;

import ing.software.dulcemayor.entity.order.Order;
import ing.software.dulcemayor.enums.OrderStatus;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends BaseRepository<Order> {

    List<Order> findByStatusAndIsDeletedFalse(OrderStatus status);

    long countByStatusAndIsDeletedFalse(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.total), 0.0) FROM Order o WHERE o.isDeleted = false AND o.status <> ing.software.dulcemayor.enums.OrderStatus.CANCELLED")
    double sumTotalSales();
}
