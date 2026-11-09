package ing.software.dulcemayor.repository.order;

import ing.software.dulcemayor.entity.order.OrderDetail;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends BaseRepository<OrderDetail> {
    List<OrderDetail> findByOrderIdAndIsDeletedFalse(String orderId);
}
