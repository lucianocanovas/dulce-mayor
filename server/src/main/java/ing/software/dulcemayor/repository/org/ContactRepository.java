package ing.software.dulcemayor.repository.org;

import ing.software.dulcemayor.entity.org.Contact;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends BaseRepository<Contact> {
    List<Contact> findByPersonIdAndIsDeletedFalse(String personId);
}
