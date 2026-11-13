package ing.software.dulcemayor.repository.catalog;

import ing.software.dulcemayor.entity.catalog.PriceValidity;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PriceValidityRepository extends BaseRepository<PriceValidity> {

    @Query("SELECT pv FROM PriceValidity pv WHERE pv.isDeleted = false AND pv.product.id = :productId AND pv.endDate IS NULL")
    Optional<PriceValidity> findActivePriceByProductId(String productId);

    @Query("SELECT pv FROM PriceValidity pv JOIN FETCH pv.product p WHERE pv.isDeleted = false AND p.isDeleted = false ORDER BY p.code ASC, CASE WHEN pv.endDate IS NULL THEN 0 ELSE 1 END ASC, pv.startDate DESC")
    List<PriceValidity> findAllWithProductOrderByProductAndDateDesc();

    @Query("SELECT pv FROM PriceValidity pv JOIN FETCH pv.product p WHERE pv.isDeleted = false AND p.id = :productId ORDER BY CASE WHEN pv.endDate IS NULL THEN 0 ELSE 1 END ASC, pv.startDate DESC")
    List<PriceValidity> findByProductIdOrderByDateDesc(String productId);
}
