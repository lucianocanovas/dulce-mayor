package ing.software.dulcemayor.repository.location;

import ing.software.dulcemayor.entity.location.City;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends BaseRepository<City> {
    List<City> findByDepartmentIdAndIsDeletedFalse(String departmentId);
}
