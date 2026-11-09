package ing.software.dulcemayor.repository.actor;

import ing.software.dulcemayor.entity.actor.Employee;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends BaseRepository<Employee> {
    Optional<Employee> findByEmployeeIdNumberAndIsDeletedFalse(String employeeIdNumber);
}
