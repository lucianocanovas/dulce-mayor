package ing.software.dulcemayor.repository.location;

import ing.software.dulcemayor.entity.location.Department;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends BaseRepository<Department> {
    List<Department> findByStateIdAndIsDeletedFalse(String stateId);
}
