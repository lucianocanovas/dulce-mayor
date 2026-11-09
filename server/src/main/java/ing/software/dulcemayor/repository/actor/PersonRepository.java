package ing.software.dulcemayor.repository.actor;

import ing.software.dulcemayor.entity.actor.Person;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonRepository extends BaseRepository<Person> {
    Optional<Person> findByDocumentNumberAndIsDeletedFalse(String documentNumber);
}
