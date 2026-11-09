package ing.software.dulcemayor.repository.catalog;

import ing.software.dulcemayor.entity.catalog.SubCategory;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubCategoryRepository extends BaseRepository<SubCategory> {
    List<SubCategory> findByCategoryIdAndIsDeletedFalse(String categoryId);
}
