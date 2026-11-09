package ing.software.dulcemayor.repository.catalog;

import ing.software.dulcemayor.entity.catalog.Product;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends BaseRepository<Product> {

    Optional<Product> findByCodeAndIsDeletedFalse(String code);

    boolean existsByCodeAndIsDeletedFalse(String code);

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND p.subCategory.id = :subCategoryId")
    List<Product> findBySubCategoryId(String subCategoryId);
}
