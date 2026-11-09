package ing.software.dulcemayor.repository.catalog;

import ing.software.dulcemayor.entity.catalog.Stock;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends BaseRepository<Stock> {

    Optional<Stock> findByProductIdAndIsDeletedFalse(String productId);

    @Query("SELECT s FROM Stock s WHERE s.isDeleted = false AND s.currentQuantity <= :threshold")
    List<Stock> findLowStock(int threshold);
}
