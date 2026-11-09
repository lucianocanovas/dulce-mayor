package ing.software.dulcemayor.repository.location;

import ing.software.dulcemayor.entity.location.Country;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CountryRepository extends BaseRepository<Country> {
    Optional<Country> findByNameAndIsDeletedFalse(String name);
}
