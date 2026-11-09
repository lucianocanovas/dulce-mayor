package ing.software.dulcemayor.repository.catalog;

import ing.software.dulcemayor.entity.catalog.Category;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends BaseRepository<Category> {

    Optional<Category> findByNameAndIsDeletedFalse(String name);
}
