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

    List<Order> findByPointOfSaleIdAndIsDeletedFalse(String pointOfSaleId);

    List<Order> findBySalesRepIdAndIsDeletedFalse(String salesRepId);

    long countByStatusAndIsDeletedFalse(OrderStatus status);

    @Query("SELECT COALESCE(MAX(o.orderNumber), 1000) FROM Order o")
    Long findMaxOrderNumber();

    @Query("SELECT COALESCE(SUM(o.total), 0.0) FROM Order o WHERE o.isDeleted = false AND o.status <> ing.software.dulcemayor.enums.OrderStatus.CANCELLED AND o.status <> ing.software.dulcemayor.enums.OrderStatus.PENDING_PAYMENT")
    double sumTotalSales();

    java.util.Optional<Order> findByMpPreferenceIdAndIsDeletedFalse(String mpPreferenceId);

    java.util.Optional<Order> findByMpPaymentIdAndIsDeletedFalse(String mpPaymentId);

    java.util.Optional<Order> findByOrderNumberAndIsDeletedFalse(Long orderNumber);

    List<Order> findByReadyForDispatchTrueAndIsDeletedFalseOrderByDispatchEnabledAtDesc();

    List<Order> findByCustomerEmailIgnoreCaseAndIsDeletedFalseOrderByOrderDateTimeDesc(String customerEmail);

    List<Order> findByIsDeletedFalseOrderByOrderDateTimeDesc();
}
