package ing.software.dulcemayor.repository.logistics;

import ing.software.dulcemayor.entity.logistics.DeliveryRoute;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryRouteRepository extends BaseRepository<DeliveryRoute> {
    List<DeliveryRoute> findByDeliveryAgentIdAndIsDeletedFalse(String deliveryAgentId);

    java.util.Optional<DeliveryRoute> findFirstByIsDeletedFalseOrderByDateDesc();

    List<DeliveryRoute> findByDateAndIsDeletedFalse(java.time.LocalDate date);
}
