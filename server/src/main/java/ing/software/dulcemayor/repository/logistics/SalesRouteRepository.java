package ing.software.dulcemayor.repository.logistics;

import ing.software.dulcemayor.entity.logistics.SalesRoute;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesRouteRepository extends BaseRepository<SalesRoute> {
    List<SalesRoute> findBySalesRepIdAndIsDeletedFalse(String salesRepId);
}
