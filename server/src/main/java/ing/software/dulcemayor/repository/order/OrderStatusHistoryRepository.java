package ing.software.dulcemayor.repository.order;

import ing.software.dulcemayor.entity.order.OrderStatusHistory;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends BaseRepository<OrderStatusHistory> {
    List<OrderStatusHistory> findByOrderIdAndIsDeletedFalseOrderByDateTimeDesc(String orderId);
}
