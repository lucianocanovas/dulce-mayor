package ing.software.dulcemayor.repository.billing;

import ing.software.dulcemayor.entity.billing.PaymentMethod;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentMethodRepository extends BaseRepository<PaymentMethod> {
}
