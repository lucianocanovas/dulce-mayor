package ing.software.dulcemayor.repository.org;

import ing.software.dulcemayor.entity.org.Wholesaler;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WholesalerRepository extends BaseRepository<Wholesaler> {
    Optional<Wholesaler> findByTaxIdAndIsDeletedFalse(String taxId);
}
