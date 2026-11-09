package ing.software.dulcemayor.repository.org;

import ing.software.dulcemayor.entity.org.CompanyEmailConfig;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyEmailConfigRepository extends BaseRepository<CompanyEmailConfig> {
    Optional<CompanyEmailConfig> findByCompanyIdAndIsDeletedFalse(String companyId);
}
