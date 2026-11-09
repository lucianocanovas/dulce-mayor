package ing.software.dulcemayor.repository.order;

import ing.software.dulcemayor.entity.order.PointOfSale;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointOfSaleRepository extends BaseRepository<PointOfSale> {
    List<PointOfSale> findByRetailerIdAndIsDeletedFalse(String retailerId);
    List<PointOfSale> findBySalesRepIdAndIsDeletedFalse(String salesRepId);
}
