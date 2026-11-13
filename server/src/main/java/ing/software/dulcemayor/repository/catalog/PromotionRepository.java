package ing.software.dulcemayor.repository.catalog;

import ing.software.dulcemayor.entity.catalog.Promotion;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PromotionRepository extends BaseRepository<Promotion> {

    @Query("SELECT p FROM Promotion p WHERE p.isDeleted = false AND UPPER(p.code) = UPPER(:code) AND :today BETWEEN p.startDate AND p.endDate")
    Optional<Promotion> findActivePromotionByCode(String code, LocalDate today);

    Optional<Promotion> findFirstByCodeIgnoreCaseAndIsDeletedFalse(String code);
}
