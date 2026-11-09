package ing.software.dulcemayor.repository.location;

import ing.software.dulcemayor.entity.location.State;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StateRepository extends BaseRepository<State> {
    List<State> findByCountryIdAndIsDeletedFalse(String countryId);
}
