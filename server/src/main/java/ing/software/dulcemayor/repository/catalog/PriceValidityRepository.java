package ing.software.dulcemayor.repository.catalog;

import ing.software.dulcemayor.entity.catalog.PriceValidity;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PriceValidityRepository extends BaseRepository<PriceValidity> {

    @Query("SELECT pv FROM PriceValidity pv WHERE pv.isDeleted = false AND pv.product.id = :productId AND pv.endDate IS NULL")
    Optional<PriceValidity> findActivePriceByProductId(String productId);
}
