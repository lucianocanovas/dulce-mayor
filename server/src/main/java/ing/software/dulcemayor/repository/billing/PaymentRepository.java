package ing.software.dulcemayor.repository.billing;

import ing.software.dulcemayor.entity.billing.Payment;
import ing.software.dulcemayor.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends BaseRepository<Payment> {
    List<Payment> findByInvoiceIdAndIsDeletedFalse(String invoiceId);

    boolean existsByReferenceAndIsDeletedFalse(String reference);

    java.util.Optional<Payment> findByReferenceAndIsDeletedFalse(String reference);
}
